package me.vel.courier.manager;

import me.vel.courier.Courier;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DataManager {

    private final Courier plugin;
    private Connection connection;
    private final File dbFile;

    public DataManager(Courier plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "data.db");
        connect();
        createTables();
        migrateOldData();
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to connect to SQLite database!");
            e.printStackTrace();
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS linked_accounts (" +
                    "player_uuid TEXT PRIMARY KEY, discord_id INTEGER NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS rewards (" +
                    "player_uuid TEXT PRIMARY KEY, received INTEGER NOT NULL DEFAULT 0)");
            stmt.execute("CREATE TABLE IF NOT EXISTS invite_cooldowns (" +
                    "player_uuid TEXT PRIMARY KEY, timestamp INTEGER NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS link_timestamps (" +
                    "player_uuid TEXT PRIMARY KEY, timestamp INTEGER NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS link_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, " +
                    "discord_id INTEGER NOT NULL, timestamp INTEGER NOT NULL, action TEXT NOT NULL)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables!");
            e.printStackTrace();
        }
    }

    private void migrateOldData() {
        File oldFile = new File(plugin.getDataFolder(), "data.yml");
        if (!oldFile.exists()) return;

        plugin.getLogger().info("Migrating data from data.yml to SQLite...");
        YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldFile);

        try {
            connection.setAutoCommit(false);

            if (oldConfig.contains("linked_accounts")) {
                String psql = "INSERT OR IGNORE INTO linked_accounts (player_uuid, discord_id) VALUES (?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(psql)) {
                    for (String key : oldConfig.getConfigurationSection("linked_accounts").getKeys(false)) {
                        ps.setString(1, key);
                        ps.setLong(2, oldConfig.getLong("linked_accounts." + key));
                        ps.executeUpdate();
                    }
                }
            }

            if (oldConfig.contains("rewards")) {
                String psql = "INSERT OR IGNORE INTO rewards (player_uuid, received) VALUES (?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(psql)) {
                    for (String key : oldConfig.getConfigurationSection("rewards").getKeys(false)) {
                        ps.setString(1, key);
                        ps.setInt(2, oldConfig.getBoolean("rewards." + key) ? 1 : 0);
                        ps.executeUpdate();
                    }
                }
            }

            if (oldConfig.contains("invite_cooldowns")) {
                String psql = "INSERT OR IGNORE INTO invite_cooldowns (player_uuid, timestamp) VALUES (?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(psql)) {
                    for (String key : oldConfig.getConfigurationSection("invite_cooldowns").getKeys(false)) {
                        ps.setString(1, key);
                        ps.setLong(2, oldConfig.getLong("invite_cooldowns." + key));
                        ps.executeUpdate();
                    }
                }
            }

            if (oldConfig.contains("link_timestamps")) {
                String psql = "INSERT OR IGNORE INTO link_timestamps (player_uuid, timestamp) VALUES (?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(psql)) {
                    for (String key : oldConfig.getConfigurationSection("link_timestamps").getKeys(false)) {
                        ps.setString(1, key);
                        ps.setLong(2, oldConfig.getLong("link_timestamps." + key));
                        ps.executeUpdate();
                    }
                }
            }

            if (oldConfig.contains("link_history")) {
                String psql = "INSERT INTO link_history (player_uuid, discord_id, timestamp, action) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(psql)) {
                    for (String playerKey : oldConfig.getConfigurationSection("link_history").getKeys(false)) {
                        String path = "link_history." + playerKey;
                        for (String entryKey : oldConfig.getConfigurationSection(path).getKeys(false)) {
                            ps.setString(1, playerKey);
                            ps.setLong(2, oldConfig.getLong(path + "." + entryKey + ".discord_id"));
                            ps.setLong(3, oldConfig.getLong(path + "." + entryKey + ".timestamp"));
                            ps.setString(4, oldConfig.getString(path + "." + entryKey + ".action"));
                            ps.executeUpdate();
                        }
                    }
                }
            }

            connection.commit();
            connection.setAutoCommit(true);

            oldFile.delete();
            plugin.getLogger().info("Migration complete! Old data.yml has been removed.");
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {}
            plugin.getLogger().severe("Failed to migrate data from data.yml!");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection!");
            e.printStackTrace();
        }
    }

    public void saveData() {
    }

    public void reloadData() {
    }

    public boolean hasReceivedLinkReward(UUID playerUuid) {
        String sql = "SELECT received FROM rewards WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("received") == 1;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check reward status!");
            e.printStackTrace();
        }
        return false;
    }

    public void setReceivedLinkReward(UUID playerUuid, boolean received) {
        String sql = "INSERT OR REPLACE INTO rewards (player_uuid, received) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, received ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set reward status!");
            e.printStackTrace();
        }
    }

    public Set<UUID> getAllRewardedPlayers() {
        Set<UUID> rewardedPlayers = new HashSet<>();
        String sql = "SELECT player_uuid FROM rewards WHERE received = 1";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    rewardedPlayers.add(UUID.fromString(rs.getString("player_uuid")));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in rewards table: " + rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get rewarded players!");
            e.printStackTrace();
        }
        return rewardedPlayers;
    }

    public void saveLinkedAccounts(Map<UUID, Long> linkedAccounts) {
        String deleteSql = "DELETE FROM linked_accounts";
        String insertSql = "INSERT INTO linked_accounts (player_uuid, discord_id) VALUES (?, ?)";
        try {
            connection.setAutoCommit(false);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(deleteSql);
            }
            try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                for (Map.Entry<UUID, Long> entry : linkedAccounts.entrySet()) {
                    ps.setString(1, entry.getKey().toString());
                    ps.setLong(2, entry.getValue());
                    ps.executeUpdate();
                }
            }
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {}
            plugin.getLogger().severe("Failed to save linked accounts!");
            e.printStackTrace();
        }
    }

    public Map<UUID, Long> loadLinkedAccounts() {
        Map<UUID, Long> linkedAccounts = new HashMap<>();
        String sql = "SELECT player_uuid, discord_id FROM linked_accounts";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    long discordId = rs.getLong("discord_id");
                    linkedAccounts.put(uuid, discordId);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in linked_accounts: " + rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load linked accounts!");
            e.printStackTrace();
        }
        return linkedAccounts;
    }

    public long getInviteCooldown(UUID playerUuid) {
        String sql = "SELECT timestamp FROM invite_cooldowns WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("timestamp");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get invite cooldown!");
            e.printStackTrace();
        }
        return 0L;
    }

    public void setInviteCooldown(UUID playerUuid, long timestamp) {
        String sql = "INSERT OR REPLACE INTO invite_cooldowns (player_uuid, timestamp) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, timestamp);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set invite cooldown!");
            e.printStackTrace();
        }
    }

    public boolean isOnCooldown(UUID playerUuid) {
        long lastInvite = getInviteCooldown(playerUuid);
        long cooldownMinutes = plugin.getConfig().getLong("discord_bot.invite_cooldown_minutes", 30);
        long cooldownMillis = cooldownMinutes * 60 * 1000;
        return System.currentTimeMillis() - lastInvite < cooldownMillis;
    }

    public long getRemainingCooldown(UUID playerUuid) {
        long lastInvite = getInviteCooldown(playerUuid);
        long cooldownMinutes = plugin.getConfig().getLong("discord_bot.invite_cooldown_minutes", 30);
        long cooldownMillis = cooldownMinutes * 60 * 1000;
        long elapsed = System.currentTimeMillis() - lastInvite;
        return Math.max(0, cooldownMillis - elapsed);
    }

    public void setLinkTimestamp(UUID playerUuid, long timestamp) {
        String sql = "INSERT OR REPLACE INTO link_timestamps (player_uuid, timestamp) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, timestamp);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to set link timestamp!");
            e.printStackTrace();
        }
    }

    public long getLinkTimestamp(UUID playerUuid) {
        String sql = "SELECT timestamp FROM link_timestamps WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getLong("timestamp");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get link timestamp!");
            e.printStackTrace();
        }
        return 0L;
    }

    public void addLinkHistory(UUID playerUuid, long discordId, long timestamp, String action) {
        String sql = "INSERT INTO link_history (player_uuid, discord_id, timestamp, action) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setLong(2, discordId);
            ps.setLong(3, timestamp);
            ps.setString(4, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add link history!");
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> getLinkHistory(UUID playerUuid) {
        List<Map<String, Object>> history = new ArrayList<>();
        String sql = "SELECT discord_id, timestamp, action FROM link_history WHERE player_uuid = ? ORDER BY id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("discord_id", rs.getLong("discord_id"));
                entry.put("timestamp", rs.getLong("timestamp"));
                entry.put("action", rs.getString("action"));
                history.add(entry);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get link history!");
            e.printStackTrace();
        }
        return history;
    }
}

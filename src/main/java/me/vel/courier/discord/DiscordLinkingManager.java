package me.vel.courier.discord;

import me.vel.courier.Courier;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordLinkingManager {

    private Courier plugin;
    private final Map<String, UUID> inviteToPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Long> linkedAccounts = new ConcurrentHashMap<>();
    private final Map<Long, UUID> discordToMinecraft = new ConcurrentHashMap<>();
    
    public void setPlugin(Courier plugin) {
        this.plugin = plugin;
        loadLinkedAccounts();
    }
    
    public void registerInvite(String inviteCode, UUID playerUuid) {
        inviteToPlayer.put(inviteCode, playerUuid);
        Bukkit.getLogger().info("[LinkingManager] Registered: " + inviteCode + " -> " + playerUuid);
    }
    
    public UUID getPlayerFromInvite(String inviteCode) {
        UUID result = inviteToPlayer.get(inviteCode);
        Bukkit.getLogger().info("[LinkingManager] Lookup " + inviteCode + " -> " + result);
        return result;
    }
    
    public boolean linkAccount(UUID playerUuid, long discordId) {
        Bukkit.getLogger().info("[LinkingManager] Linking " + playerUuid + " to Discord " + discordId);
        
        if (linkedAccounts.containsKey(playerUuid)) {
            Bukkit.getLogger().warning("[LinkingManager] UUID already linked!");
            return false;
        }
        
        if (discordToMinecraft.containsKey(discordId)) {
            Bukkit.getLogger().warning("[LinkingManager] Discord ID already linked!");
            return false;
        }
        
        linkedAccounts.put(playerUuid, discordId);
        discordToMinecraft.put(discordId, playerUuid);
        Bukkit.getLogger().info("[LinkingManager] ✓ Link successful! Total: " + linkedAccounts.size());
        
        if (plugin != null) {
            plugin.getDataManager().saveLinkedAccounts(linkedAccounts);
        }
        
        return true;
    }
    
    public boolean isLinked(UUID playerUuid) {
        boolean linked = linkedAccounts.containsKey(playerUuid);
        Bukkit.getLogger().info("[LinkingManager] Is " + playerUuid + " linked? " + linked);
        return linked;
    }
    
    public Long getDiscordId(UUID playerUuid) {
        return linkedAccounts.get(playerUuid);
    }
    
    public UUID getMinecraftUuid(long discordId) {
        return discordToMinecraft.get(discordId);
    }
    
    public void removeInvite(String inviteCode) {
        inviteToPlayer.remove(inviteCode);
    }
    
    public boolean unlinkAccount(UUID playerUuid) {
        Long discordId = linkedAccounts.remove(playerUuid);
        if (discordId != null) {
            discordToMinecraft.remove(discordId);
            Bukkit.getLogger().info("[LinkingManager] Unlinked " + playerUuid + " from Discord " + discordId);
            
            if (plugin != null) {
                plugin.getDataManager().saveLinkedAccounts(linkedAccounts);
            }
            
            return true;
        }
        return false;
    }
    
    public boolean unlinkDiscordAccount(long discordId) {
        UUID playerUuid = discordToMinecraft.remove(discordId);
        if (playerUuid != null) {
            linkedAccounts.remove(playerUuid);
            Bukkit.getLogger().info("[LinkingManager] Unlinked Discord " + discordId + " from " + playerUuid);
            
            if (plugin != null) {
                plugin.getDataManager().saveLinkedAccounts(linkedAccounts);
            }
            
            return true;
        }
        return false;
    }
    
    public Map<UUID, Long> getLinkedAccounts() {
        return new ConcurrentHashMap<>(linkedAccounts);
    }
    
    public void loadLinkedAccounts(Map<UUID, Long> accounts) {
        linkedAccounts.clear();
        discordToMinecraft.clear();
        linkedAccounts.putAll(accounts);
        accounts.forEach((uuid, discordId) -> discordToMinecraft.put(discordId, uuid));
    }
    
    private void loadLinkedAccounts() {
        if (plugin != null) {
            Map<UUID, Long> saved = plugin.getDataManager().loadLinkedAccounts();
            loadLinkedAccounts(saved);
            Bukkit.getLogger().info("[LinkingManager] Loaded " + saved.size() + " linked accounts from database");
        }
    }
    
    public int getRegisteredInviteCount() {
        return inviteToPlayer.size();
    }
    
    public void debugPrintInvites() {
        Bukkit.getLogger().info("[LinkingManager] === REGISTERED INVITES ===");
        inviteToPlayer.forEach((code, uuid) -> {
            Bukkit.getLogger().info("[LinkingManager] " + code + " -> " + uuid);
        });
        Bukkit.getLogger().info("[LinkingManager] Total: " + inviteToPlayer.size());
    }
}

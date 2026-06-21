package me.vel.courier.discord;

import me.vel.courier.Courier;
import me.vel.courier.discord.discord_commands.*;
import me.vel.courier.discord.discord_events.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.awt.*;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class DiscordBot extends ListenerAdapter {

    private final Courier plugin;
    private final DiscordLinkingManager linkingManager;
    private JDA jda;
    private boolean enabled = false;
    
    private TextChannel minecraftChatChannel;
    private TextChannel consoleChannel;
    private TextChannel reportChannel;
    private TextChannel reportLogsChannel;
    private TextChannel statusChannel;
    private Guild guild;
    private Role staffRole;
    
    private final Map<String, Integer> inviteUses = new ConcurrentHashMap<>();
    
    private AnnouncementCommand announcementCommand;
    private BulkUnlinkCommand bulkUnlinkCommand;
    private LinkedAccountsCommand linkedAccountsCommand;
    
    public DiscordBot(Courier plugin, DiscordLinkingManager linkingManager) {
        this.plugin = plugin;
        this.linkingManager = linkingManager;
    }
    
    private Activity buildActivity() {
        String type = plugin.getConfig().getString("discord_bot.activity_type", "playing").toLowerCase();
        String text = plugin.getConfig().getString("discord_bot.activity_text", "MyServer");
        return switch (type) {
            case "watching" -> Activity.watching(text);
            case "listening" -> Activity.listening(text);
            case "streaming" -> Activity.streaming(text, "https://twitch.tv/");
            case "competing" -> Activity.competing(text);
            default -> Activity.playing(text);
        };
    }

    public void startBot() {
        String token = plugin.getConfig().getString("discord_bot.token", "");
        
        if (token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("Discord Courier token not configured! Discord integration disabled.");
            plugin.getLogger().warning("Please set 'discord_bot.token' in config.yml");
            return;
        }
        
        try {
            jda = JDABuilder.createLight(token, EnumSet.of(
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.GUILD_INVITES,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT
            ))
                    .addEventListeners(this)
                    .setActivity(buildActivity())
                    .build();
            
            jda.awaitReady();
            
            String guildId = plugin.getConfig().getString("discord_bot.guild_id", "");
            if (!guildId.isEmpty()) {
                guild = jda.getGuildById(guildId);
                if (guild != null) {
                    String chatChannelId = plugin.getConfig().getString("discord_bot.channels.minecraft_chat", "");
                    String consoleChannelId = plugin.getConfig().getString("discord_bot.channels.console", "");
                    String reportChannelId = plugin.getConfig().getString("discord_bot.channels.reports", "");
                    String reportLogsChannelId = plugin.getConfig().getString("discord_bot.channels.report_logs", "");
                    String statusChannelId = plugin.getConfig().getString("discord_bot.channels.status", "");
                    String staffRoleId = plugin.getConfig().getString("discord_bot.staff_role_id", "");
                    
                    if (!chatChannelId.isEmpty()) {
                        minecraftChatChannel = guild.getTextChannelById(chatChannelId);
                    }
                    
                    if (!consoleChannelId.isEmpty()) {
                        consoleChannel = guild.getTextChannelById(consoleChannelId);
                    }
                    
                    if (!reportChannelId.isEmpty()) {
                        reportChannel = guild.getTextChannelById(reportChannelId);
                    }

                    if (!reportLogsChannelId.isEmpty()) {
                        reportLogsChannel = guild.getTextChannelById(reportLogsChannelId);
                    }
                    
                    if (!statusChannelId.isEmpty()) {
                        statusChannel = guild.getTextChannelById(statusChannelId);
                    }
                    
                    if (!staffRoleId.isEmpty() && !staffRoleId.equals("YOUR_STAFF_ROLE_ID_HERE")) {
                        staffRole = guild.getRoleById(staffRoleId);
                    }
                    jda.addEventListener(new GuildMemberJoinListener(plugin, this, linkingManager, inviteUses, guild));
                    jda.addEventListener(new GuildMemberRemoveListener(plugin, linkingManager, guild));
                    jda.addEventListener(new MessageReceivedListener(plugin, minecraftChatChannel, consoleChannel));
                    jda.addEventListener(new SlashCommandListener(plugin));
                    jda.addEventListener(new StringSelectListener(this));

                    announcementCommand = new AnnouncementCommand(plugin);
                    bulkUnlinkCommand = new BulkUnlinkCommand(plugin);
                    linkedAccountsCommand = new LinkedAccountsCommand(plugin);
                }
            }
            
            enabled = true;
            plugin.getLogger().info("Discord Courier connected successfully!");
            
            registerCommands();
            cacheInviteUses();
            sendServerOnlineStatus();
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start Discord bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void registerCommands() {
        if (guild == null) {
            plugin.getLogger().warning("Cannot register Discord commands - guild not found!");
            return;
        }
        
        guild.updateCommands()
            .addCommands(
                Commands.slash("serverstats", "Show server statistics")
                    .setDefaultPermissions(DefaultMemberPermissions.ENABLED),
                Commands.slash("announce", "Create an announcement (Admin only)")
                    .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("bulkunlink", "Unlink all accounts (Admin only)")
                    .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("linkhistory", "View a player's link history (Moderator+)")
                    .addOption(OptionType.STRING, "player", "The player's name", true)
                    .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("reports", "View reports (Moderator+)")
                    .addOption(OptionType.STRING, "type", "resolved or unresolved", true)
                    .setDefaultPermissions(DefaultMemberPermissions.DISABLED),
                Commands.slash("linkedaccounts", "View or check linked accounts")
                    .addSubcommands(
                        new SubcommandData("list", "View all linked accounts"),
                        new SubcommandData("check", "Check if a player is linked")
                            .addOption(OptionType.STRING, "player", "The player's name", true)
                    )
                    .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
            )
            .queue(
                success -> plugin.getLogger().info("Discord slash commands registered successfully!"),
                error -> plugin.getLogger().warning("Failed to register Discord commands: " + error.getMessage())
            );
    }
    
    public void cacheInviteUses() {
        if (guild == null) {
            return;
        }
        
        guild.retrieveInvites().queue(invites -> {
            inviteUses.clear();
            for (Invite invite : invites) {
                inviteUses.put(invite.getCode(), invite.getUses());
            }
        });
    }
    
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("announcement_modal")) {
            announcementCommand.handleModal(event);
        }
    }
    
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (buttonId.startsWith("bulkunlink_")) {
            bulkUnlinkCommand.handleButton(event);
        }
    }
    
    public void stopBot() {
        if (jda != null) {
            sendServerOfflineStatus();
            jda.shutdown();
            enabled = false;
            plugin.getLogger().info("Discord Courier disconnected.");
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }

    public void sendMinecraftChat(String playerName, String message) {
        if (minecraftChatChannel != null) {
            String format = plugin.getConfig().getString("discord_bot.chat_format.mc_to_discord", "**%player%**: %message%");
            String timestamp = plugin.getTimeManager().getCurrentTimestamp();
            String formatted = format
                    .replace("%player%", playerName)
                    .replace("%message%", message)
                    .replace("{timestamp}", timestamp);
            minecraftChatChannel.sendMessage(formatted).queue();
        }
    }

    public void sendConsoleMessage(String message) {
        if (consoleChannel != null) {
            if (message.length() > 2000) {
                message = message.substring(0, 1997) + "...";
            }
            consoleChannel.sendMessage("```\n" + message + "\n```").queue();
        }
    }
    
    public void sendToDiscord(TextChannel channel, String message) {
        if (channel != null && enabled) {
            if (message.length() > 2000) {
                message = message.substring(0, 1997) + "...";
            }
            channel.sendMessage(message).queue();
        }
    }
    
    public void sendReport(String reporter, String reported, String reason) {
        if (reportChannel == null) {
            return;
        }
        
        String timestamp = plugin.getTimeManager().getCurrentTimestamp();
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📋 New Player Report");
        embed.setColor(Color.RED);
        embed.addField("Reporter", reporter, true);
        embed.addField("Reported Player", reported, true);
        embed.addField("Reason", reason, false);
        embed.addField("Time", timestamp, false);
        embed.setTimestamp(Instant.now());
        embed.setFooter(plugin.getServerName() + " Report System");
        
        String mention = staffRole != null ? staffRole.getAsMention() + " " : "";
        reportChannel.sendMessage(mention).setEmbeds(embed.build()).queue();
    }
    
    public void sendCommandLog(String playerName, String command) {
        if (consoleChannel != null) {
            String timestamp = plugin.getTimeManager().getCurrentTimestamp();
            String message = "**[" + timestamp + "]** `" + playerName + "` executed: `/" + command + "`";
            consoleChannel.sendMessage(message).queue();
        }
    }
    
    public void createInviteForPlayer(UUID playerUuid, String playerName, java.util.function.Consumer<String> callback) {
        if (guild == null) {
            callback.accept(null);
            return;
        }
        
        String inviteChannelId = plugin.getConfig().getString("discord_bot.invite_channel_id", "");
        
        if (inviteChannelId.isEmpty() || inviteChannelId.equals("YOUR_INVITE_CHANNEL_ID_HERE")) {
            callback.accept(null);
            return;
        }
        
        TextChannel inviteChannel = guild.getTextChannelById(inviteChannelId);
        if (inviteChannel == null) {
            callback.accept(null);
            return;
        }
        
        inviteChannel.createInvite()
                .setMaxUses(1)
                .setMaxAge(24L, TimeUnit.HOURS)
                .setUnique(true)
                .queue(invite -> {
                    linkingManager.registerInvite(invite.getCode(), playerUuid);
                    inviteUses.put(invite.getCode(), 0);
                    callback.accept(invite.getUrl());
                }, error -> {
                    plugin.getLogger().severe("Failed to create invite: " + error.getMessage());
                    callback.accept(null);
                });
    }

    public void giveRewards(Player player) {
        boolean itemEnabled = plugin.getConfig().getBoolean("discord_bot.rewards.item.enabled", true);

        if (itemEnabled) {
            giveRewardItem(player);
        }

        if (!player.isOnline()) {
            plugin.getLogger().warning("Player " + player.getName() + " is not online, cannot give rewards");
            return;
        }

        List<String> commands = plugin.getConfig().getStringList("discord_bot.rewards.commands");
        if (!commands.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (String command : commands) {
                    String processedCommand = command.replace("%player%", player.getName());
                    boolean result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);

                    if (!result) {
                        plugin.getLogger().warning("Command failed, trying with elevated permissions...");
                    }
                }
            });
        }
    }
    
    private void giveRewardItem(Player player) {
        String materialName = plugin.getConfig().getString("discord_bot.rewards.item.material", "DIAMOND");
        int amount = plugin.getConfig().getInt("discord_bot.rewards.item.amount", 1);
        String displayName = plugin.getConfig().getString("discord_bot.rewards.item.name", "&b&lDiscord Link Reward");
        List<String> lore = plugin.getConfig().getStringList("discord_bot.rewards.item.lore");
        
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.DIAMOND;
        }
        
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(displayName.replace("&", "§"));
            if (!lore.isEmpty()) {
                meta.setLore(lore.stream().map(line -> line.replace("&", "§")).toList());
            }
            item.setItemMeta(meta);
        }
        
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItem(player.getLocation(), item);
            player.sendMessage(plugin.getPrefix() + " §eYour inventory is full! The reward was dropped on the ground.");
        } else {
            player.getInventory().addItem(item);
        }
    }

    
    public TextChannel getMinecraftChatChannel() {
        return minecraftChatChannel;
    }
    
    public TextChannel getConsoleChannel() {
        return consoleChannel;
    }
    
    public TextChannel getStatusChannel() {
        return statusChannel;
    }
    
    public JDA getJDA() {
        return jda;
    }
    
    public Guild getGuild() {
        return guild;
    }
    
    public void removeRoleFromUser(Long discordId) {
        if (guild == null || discordId == null) {
            return;
        }
        
        String roleId = plugin.getConfig().getString("discord_bot.linked_role_id", "");
        if (roleId.isEmpty() || roleId.equals("YOUR_LINKED_ROLE_ID_HERE")) {
            return;
        }
        
        Role linkedRole = guild.getRoleById(roleId);
        if (linkedRole == null) {
            return;
        }
        
        guild.retrieveMemberById(discordId).queue(member -> {
            if (member != null) {
                guild.removeRoleFromMember(member, linkedRole).queue(
                    success -> plugin.getLogger().info("Removed linked role from Discord user " + discordId),
                    error -> plugin.getLogger().warning("Failed to remove role: " + error.getMessage())
                );
            }
        }, error -> {
            plugin.getLogger().info("User " + discordId + " not found in guild (already left)");
        });
    }
    
    public String getDiscordUsername(long discordId) {
        if (jda == null) {
            return "Unknown (Courier Offline)";
        }
        
        try {
            User user = jda.retrieveUserById(discordId).complete();
            return user != null ? user.getName() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    public void sendServerOnlineStatus() {
        if (statusChannel == null) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("discord_bot.send_server_status", true)) {
            return;
        }
        
        String timestamp = plugin.getTimeManager().getCurrentTimestamp();
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🟢 Server Online");
        embed.setColor(Color.GREEN);
        embed.setDescription("The server is now **online** and ready for players!");
        embed.addField("Server IP", plugin.getServerIp(), false);
        embed.addField("Time", timestamp, false);
        embed.setTimestamp(Instant.now());
        embed.setFooter(plugin.getServerName() + " Server Status");
        
        statusChannel.sendMessageEmbeds(embed.build()).queue();
    }
    
    public void sendServerOfflineStatus() {
        if (statusChannel == null) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("discord_bot.send_server_status", true)) {
            return;
        }
        
        String timestamp = plugin.getTimeManager().getCurrentTimestamp();
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🔴 Server Offline");
        embed.setColor(Color.RED);
        embed.setDescription("The server is now **offline**.");
        embed.addField("Time", timestamp, false);
        embed.setTimestamp(Instant.now());
        embed.setFooter(plugin.getServerName() + " Server Status");
        
        statusChannel.sendMessageEmbeds(embed.build()).queue();
    }
    
    public void sendPlayerJoinMessage(String playerName) {
        if (statusChannel == null) {
            return;
        }
        
        if (!plugin.getConfig().getBoolean("discord_bot.send_player_joins", true)) {
            return;
        }
        
        String timestamp = plugin.getTimeManager().getCurrentTimestamp();
        
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🥳 Player Joined");
        embed.setColor(new Color(85, 255, 85));
        embed.setDescription("**" + playerName + "** has joined the server!");
        embed.addField("Time", timestamp, false);
        embed.setTimestamp(Instant.now());
        embed.setFooter(plugin.getServerName() + " Player Activity");
        
        statusChannel.sendMessageEmbeds(embed.build()).queue();
    }

    public void sendPlayerQuitMessage(String playerName) {
        if (statusChannel == null) {
            return;
        }

        if (!plugin.getConfig().getBoolean("discord_bot.send_player_joins", true)) {
            return;
        }

        String timestamp = plugin.getTimeManager().getCurrentTimestamp();

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("👋 Player Left");
        embed.setColor(new Color(85, 255, 85));
        embed.setDescription("**" + playerName + "** has left the server..");
        embed.addField("Time", timestamp, false);
        embed.setTimestamp(Instant.now());
        embed.setFooter(plugin.getServerName() + " Player Activity");

        statusChannel.sendMessageEmbeds(embed.build()).queue();
    }

    public void sendFilteredMessageLog(String playerName, String message, String channelId) {
        if (jda == null) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Filter log channel not found: " + channelId);
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.RED);
        embed.setTitle("🚫 Filtered Message");
        embed.addField("Player", playerName, true);
        embed.addField("Time", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()), true);
        embed.addField("Message", "```" + message + "```", false);
        embed.setFooter("Chat Filter System");
        embed.setTimestamp(Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }
}

package me.vel.courier.api;

import me.vel.courier.Courier;
import me.vel.courier.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;

import java.awt.*;
import java.time.Instant;
import java.util.function.Consumer;

public class DiscordAPI {
    
    private final Courier plugin;
    private final DiscordBot discordBot;
    
    public DiscordAPI(Courier plugin, DiscordBot discordBot) {
        this.plugin = plugin;
        this.discordBot = discordBot;
    }

    public void sendMessage(String channelId, String message) {
        if (!discordBot.isEnabled()) {
            plugin.getLogger().warning("Cannot send message - Discord Courier is not enabled");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                TextChannel channel = getTextChannel(channelId);
                if (channel == null) {
                    plugin.getLogger().warning("Channel not found: " + channelId);
                    return;
                }

                String finalMessage = message.length() > 2000 ? message.substring(0, 1997) + "..." : message;

                channel.sendMessage(finalMessage).queue();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send message: " + e.getMessage());
            }
        });
    }

    public void sendMessage(String channelId, String message, Consumer<Boolean> callback) {
        if (!discordBot.isEnabled()) {
            plugin.getLogger().warning("Cannot send message - Discord Courier is not enabled");
            if (callback != null) callback.accept(false);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                TextChannel channel = getTextChannel(channelId);
                if (channel == null) {
                    plugin.getLogger().warning("Channel not found: " + channelId);
                    if (callback != null) callback.accept(false);
                    return;
                }

                String finalMessage = message.length() > 2000 ? message.substring(0, 1997) + "..." : message;

                channel.sendMessage(finalMessage).queue(
                    success -> { if (callback != null) callback.accept(true); },
                    error -> {
                        plugin.getLogger().severe("Failed to send message: " + error.getMessage());
                        if (callback != null) callback.accept(false);
                    }
                );
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send message: " + e.getMessage());
                if (callback != null) callback.accept(false);
            }
        });
    }
    
    public void sendEmbed(String channelId, MessageEmbed embed) {
        if (!discordBot.isEnabled()) {
            plugin.getLogger().warning("Cannot send embed - Discord Courier is not enabled");
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                TextChannel channel = getTextChannel(channelId);
                if (channel == null) {
                    plugin.getLogger().warning("Channel not found: " + channelId);
                    return;
                }
                
                channel.sendMessageEmbeds(embed).queue();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send embed: " + e.getMessage());
            }
        });
    }
    
    public void sendEmbed(String channelId, MessageEmbed embed, Consumer<Boolean> callback) {
        if (!discordBot.isEnabled()) {
            plugin.getLogger().warning("Cannot send embed - Discord Courier is not enabled");
            if (callback != null) callback.accept(false);
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                TextChannel channel = getTextChannel(channelId);
                if (channel == null) {
                    plugin.getLogger().warning("Channel not found: " + channelId);
                    if (callback != null) callback.accept(false);
                    return;
                }
                
                channel.sendMessageEmbeds(embed).queue(
                    success -> { if (callback != null) callback.accept(true); },
                    error -> {
                        plugin.getLogger().severe("Failed to send embed: " + error.getMessage());
                        if (callback != null) callback.accept(false);
                    }
                );
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send embed: " + e.getMessage());
                if (callback != null) callback.accept(false);
            }
        });
    }
    
    public void sendMessageWithEmbed(String channelId, String message, MessageEmbed embed) {
        if (!discordBot.isEnabled()) {
            plugin.getLogger().warning("Cannot send message with embed - Discord Courier is not enabled");
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                TextChannel channel = getTextChannel(channelId);
                if (channel == null) {
                    plugin.getLogger().warning("Channel not found: " + channelId);
                    return;
                }
                
                String finalMessage = message.length() > 2000 ? message.substring(0, 1997) + "..." : message;
                channel.sendMessage(finalMessage).setEmbeds(embed).queue();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send message with embed: " + e.getMessage());
            }
        });
    }
    
    public void sendMessageWithEmbed(String channelId, String message, MessageEmbed embed, Consumer<Boolean> callback) {
        if (!discordBot.isEnabled()) {
            plugin.getLogger().warning("Cannot send message with embed - Discord Courier is not enabled");
            if (callback != null) callback.accept(false);
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                TextChannel channel = getTextChannel(channelId);
                if (channel == null) {
                    plugin.getLogger().warning("Channel not found: " + channelId);
                    if (callback != null) callback.accept(false);
                    return;
                }
                
                String finalMessage = message.length() > 2000 ? message.substring(0, 1997) + "..." : message;
                channel.sendMessage(finalMessage).setEmbeds(embed).queue(
                    success -> { if (callback != null) callback.accept(true); },
                    error -> {
                        plugin.getLogger().severe("Failed to send message with embed: " + error.getMessage());
                        if (callback != null) callback.accept(false);
                    }
                );
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send message with embed: " + e.getMessage());
                if (callback != null) callback.accept(false);
            }
        });
    }
    
    public EmbedBuilder createEmbed() {
        return new EmbedBuilder();
    }
    
    public EmbedBuilder createEmbed(String title, String description, Color color) {
        EmbedBuilder embed = new EmbedBuilder();
        if (title != null) embed.setTitle(title);
        if (description != null) embed.setDescription(description);
        if (color != null) embed.setColor(color);
        embed.setTimestamp(Instant.now());
        return embed;
    }
    
    public EmbedBuilder createSuccessEmbed(String title, String description) {
        return createEmbed(title, description, Color.GREEN);
    }
    
    public EmbedBuilder createErrorEmbed(String title, String description) {
        return createEmbed(title, description, Color.RED);
    }
    
    public EmbedBuilder createWarningEmbed(String title, String description) {
        return createEmbed(title, description, Color.ORANGE);
    }
    
    public EmbedBuilder createInfoEmbed(String title, String description) {
        return createEmbed(title, description, Color.CYAN);
    }
    
    public String getMinecraftChatChannelId() {
        return plugin.getConfig().getString("discord_bot.channels.minecraft_chat", "");
    }
    
    public String getConsoleChannelId() {
        return plugin.getConfig().getString("discord_bot.channels.console", "");
    }
    
    public String getReportsChannelId() {
        return plugin.getConfig().getString("discord_bot.channels.reports", "");
    }
    
    public String getReportLogsChannelId() {
        return plugin.getConfig().getString("discord_bot.channels.report_logs", "");
    }
    
    
    public String getStatusChannelId() {
        return plugin.getConfig().getString("discord_bot.channels.status", "");
    }
    
    public String getFilterLogChannelId() {
        return plugin.getConfig().getString("discord_bot.channels.filter_log", "");
    }
    
    public void sendToMinecraftChat(String message) {
        sendMessage(getMinecraftChatChannelId(), message);
    }
    
    public void sendToConsole(String message) {
        sendMessage(getConsoleChannelId(), message);
    }
    
    public void sendToReports(MessageEmbed embed) {
        sendEmbed(getReportsChannelId(), embed);
    }
    
    public void sendToReportLogs(MessageEmbed embed) {
        sendEmbed(getReportLogsChannelId(), embed);
    }
    
    public void sendToStatus(MessageEmbed embed) {
        sendEmbed(getStatusChannelId(), embed);
    }
    
    public void sendToFilterLog(MessageEmbed embed) {
        sendEmbed(getFilterLogChannelId(), embed);
    }
    
    public boolean isEnabled() {
        return discordBot.isEnabled();
    }
    
    public DiscordBot getDiscordBot() {
        return discordBot;
    }
    
    public TextChannel getChannelById(String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return null;
        }
        
        try {
            if (discordBot.getJDA() != null) {
                return discordBot.getJDA().getTextChannelById(channelId);
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting text channel: " + e.getMessage());
            return null;
        }
    }
    
    private TextChannel getTextChannel(String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return null;
        }
        
        try {
            if (discordBot.getMinecraftChatChannel() != null && 
                discordBot.getMinecraftChatChannel().getId().equals(channelId)) {
                return discordBot.getMinecraftChatChannel();
            }
            
            if (discordBot.getConsoleChannel() != null && 
                discordBot.getConsoleChannel().getId().equals(channelId)) {
                return discordBot.getConsoleChannel();
            }
            
            if (discordBot.getStatusChannel() != null && 
                discordBot.getStatusChannel().getId().equals(channelId)) {
                return discordBot.getStatusChannel();
            }
            
            if (discordBot.getJDA() != null) {
                return discordBot.getJDA().getTextChannelById(channelId);
            }
            
            return null;
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting text channel: " + e.getMessage());
            return null;
        }
    }
}

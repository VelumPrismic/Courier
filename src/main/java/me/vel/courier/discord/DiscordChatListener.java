package me.vel.courier.discord;

import me.vel.courier.Courier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.regex.Pattern;

public class DiscordChatListener implements Listener {

    private final Courier plugin;
    private final DiscordBot discordBot;

    public DiscordChatListener(Courier plugin, DiscordBot discordBot) {
        this.plugin = plugin;
        this.discordBot = discordBot;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("discord_bot.relay_chat", true)) {
            return;
        }

        if (!discordBot.isEnabled()) {
            return;
        }

        String playerName = event.getPlayer().getName();
        String message = event.getMessage();

        if (shouldFilterMessage(message)) {
            logFilteredMessage(playerName, message);
            plugin.getLogger().info("Filtered message from " + playerName + " (matched filter)");
            return;
        }

        discordBot.sendMinecraftChat(playerName, message);
    }

    private boolean shouldFilterMessage(String message) {
        if (!plugin.getConfig().getBoolean("discord_bot.filter.enabled", false)) {
            return false;
        }

        boolean caseSensitive = plugin.getConfig().getBoolean("discord_bot.filter.case_sensitive", false);
        String messageToCheck = caseSensitive ? message : message.toLowerCase();

        List<String> blockedWords = plugin.getConfig().getStringList("discord_bot.filter.blocked_words");
        for (String word : blockedWords) {
            String wordToCheck = caseSensitive ? word : word.toLowerCase();
            if (messageToCheck.contains(wordToCheck)) {
                return true;
            }
        }

        List<String> blockedPatterns = plugin.getConfig().getStringList("discord_bot.filter.blocked_patterns");
        for (String patternString : blockedPatterns) {
            try {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                Pattern pattern = Pattern.compile(patternString, flags);
                if (pattern.matcher(message).find()) {
                    return true;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid regex pattern in filter: " + patternString);
            }
        }

        return false;
    }

    private void logFilteredMessage(String playerName, String message) {
        String filterLogChannelId = plugin.getConfig().getString("discord_bot.channels.filter_log");

        if (filterLogChannelId == null || filterLogChannelId.isEmpty() ||
                filterLogChannelId.equals("FILTER_LOG_CHANNEL_ID")) {
            return;
        }

        discordBot.sendFilteredMessageLog(playerName, message, filterLogChannelId);
    }
}
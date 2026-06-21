package me.vel.courier.discord.discord_events;

import me.vel.courier.Courier;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

public class MessageReceivedListener extends ListenerAdapter {

    private final Courier plugin;
    private final TextChannel minecraftChatChannel;
    private final TextChannel consoleChannel;

    public MessageReceivedListener(Courier plugin, TextChannel minecraftChatChannel, TextChannel consoleChannel) {
        this.plugin = plugin;
        this.minecraftChatChannel = minecraftChatChannel;
        this.consoleChannel = consoleChannel;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String message = event.getMessage().getContentDisplay();

        if (consoleChannel != null && event.getChannel().equals(consoleChannel)) {
            if (!plugin.getConfig().getBoolean("discord_bot.console_relay_enabled", true)) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), message);
                    event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue(
                            success -> {},
                            error -> event.getMessage().addReaction(Emoji.fromUnicode("☑️")).queue()
                    );
                } catch (Exception e) {
                    event.getChannel().sendMessage("Error executing command: " + e.getMessage()).queue();
                }
            });
            return;
        }

        if (minecraftChatChannel != null && event.getChannel().equals(minecraftChatChannel)) {
            String discordUsername = event.getAuthor().getName();

            String format = plugin.getConfig().getString("discord_bot.chat_format.discord_to_mc", "&9[Discord] &b%user%&7: %message%");
            String formatted = format.replace("%user%", discordUsername).replace("%message%", message).replace("&", "§");

            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcastMessage(formatted);
            });
        }
    }
}

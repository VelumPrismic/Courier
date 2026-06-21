package me.vel.courier.listeners;

import me.vel.courier.Courier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

    private final Courier plugin;

    public CommandListener(Courier plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("discord_bot.log_commands", true)) {
            return;
        }

        if (!plugin.getDiscordBot().isEnabled()) {
            return;
        }

        String command = event.getMessage();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        plugin.getDiscordBot().sendCommandLog(event.getPlayer().getName(), command);
    }
}

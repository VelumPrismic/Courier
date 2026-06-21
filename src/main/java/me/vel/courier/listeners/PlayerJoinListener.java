package me.vel.courier.listeners;

import me.vel.courier.Courier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final Courier plugin;

    public PlayerJoinListener(Courier plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getMessageUtil().checkPendingMessages(event.getPlayer());
        
        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().isEnabled()) {
            plugin.getDiscordBot().sendPlayerJoinMessage(event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLeave(PlayerQuitEvent event) {
        plugin.getMessageUtil().checkPendingMessages(event.getPlayer());

        if (plugin.getDiscordBot() != null && plugin.getDiscordBot().isEnabled()) {
            plugin.getDiscordBot().sendPlayerQuitMessage(event.getPlayer().getName());
        }
    }
}

package me.vel.courier.utils;

import me.vel.courier.Courier;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MessageUtil {

    private final Courier plugin;
    private final Map<UUID, List<String>> pendingUnlinkMessages = new ConcurrentHashMap<>();

    public MessageUtil(Courier plugin) {
        this.plugin = plugin;
    }

    public void sendUnlinkNotification(UUID playerUuid, String reason) {
        Player player = Bukkit.getPlayer(playerUuid);
        String prefix = plugin.getPrefix();

        if (player != null && player.isOnline()) {
            player.sendMessage("");
            player.sendMessage(prefix + " §cDiscord Account Unlinked");
            player.sendMessage(prefix + " §7Your discord account has been unliked due to " + reason);
            player.sendMessage("");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        } else {
            List<String> messages = new ArrayList<>();
            messages.add("");
            messages.add(prefix + " §cDiscord Account Unlinked");
            messages.add(prefix + " §7Your discord account has been unliked due to " + reason);
            messages.add("");
            pendingUnlinkMessages.put(playerUuid, messages);
        }
    }

    public void checkPendingMessages(Player player) {
        List<String> messages = pendingUnlinkMessages.remove(player.getUniqueId());
        if (messages != null) {
            for (String message : messages) {
                player.sendMessage(message);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        }
    }

    public void sendMessage(Player player, String message) {
        String prefix = plugin.getPrefix();
        player.sendMessage(prefix + " " + message.replace("&", "§"));
    }

    public void sendMessage(UUID playerUuid, String message) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            sendMessage(player, message);
        }
    }
}
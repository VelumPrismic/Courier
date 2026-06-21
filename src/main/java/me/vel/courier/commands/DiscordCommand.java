package me.vel.courier.commands;

import me.vel.courier.Courier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DiscordCommand implements CommandExecutor {

    private final Courier plugin;

    public DiscordCommand(Courier plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;
        String prefix = plugin.getPrefix();

        if (plugin.getLinkingManager().isLinked(player.getUniqueId())) {
            String discordLink = plugin.getConfig().getString("discord_invite_link", "https://discord.gg/yourserver");

            Component inviteComponent = Component.text(prefix + "§d§nClick here to join our Discord!")
                    .clickEvent(ClickEvent.openUrl(discordLink))
                    .hoverEvent(HoverEvent.showText(Component.text(" Click to open Discord invite.")));

            player.sendMessage("");
            player.sendMessage(prefix + " §aYour account is already linked to Discord!");
            player.sendMessage(inviteComponent);
            player.sendMessage("");

            playSound(player);
            return true;
        }

        if (!plugin.getDiscordBot().isEnabled()) {
            player.sendMessage("");
            player.sendMessage(prefix + " §cDiscord Courier is not enabled!");
            player.sendMessage(prefix + " §7Please contact an administrator.");
            player.sendMessage("");
            return true;
        }

        if (plugin.getDataManager().isOnCooldown(player.getUniqueId())) {
            long remainingMillis = plugin.getDataManager().getRemainingCooldown(player.getUniqueId());
            long minutes = remainingMillis / 60000;
            long seconds = (remainingMillis % 60000) / 1000;
            
            player.sendMessage("");
            player.sendMessage(prefix + " §cYou must wait before generating another invite!");
            player.sendMessage(prefix + " §7Time remaining: §e" + minutes + "m " + seconds + "s");
            player.sendMessage("");
            return true;
        }

        player.sendMessage("");
        player.sendMessage(prefix + " §5Generating your unique Discord invite...");
        player.sendMessage(prefix + " §7Please wait...");
        player.sendMessage("");
        
        plugin.getDataManager().setInviteCooldown(player.getUniqueId(), System.currentTimeMillis());

        plugin.getDiscordBot().createInviteForPlayer(player.getUniqueId(), player.getName(), inviteUrl -> {
            if (inviteUrl == null) {
                player.sendMessage("");
                player.sendMessage(prefix + " §cFailed to create Discord invite!");
                player.sendMessage(prefix + " §7Please contact an administrator.");
                player.sendMessage("");
                return;
            }

            Component inviteComponent = Component.text("§d§nClick here to join!")
                    .clickEvent(ClickEvent.openUrl(inviteUrl))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to open your unique invite")));

            player.sendMessage("");
            player.sendMessage(prefix + " §5Your unique Discord invite has been created!");
            player.sendMessage(prefix + " §7This invite is §conly for you§7 and expires in §e24 hours§7.");
            player.sendMessage("");
            player.sendMessage(inviteComponent);
            player.sendMessage("");
            player.sendMessage(prefix + " §cDo not share this link with anyone! You'd be linking your minecraft account to their discord account!");
            player.sendMessage("");

            playSound(player);
        });

        return true;
    }

    private void playSound(Player player) {
        try {
            String soundName = plugin.getConfig().getString("discord.sound", "BLOCK_NOTE_BLOCK_PLING");
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + plugin.getConfig().getString("discord.sound"));
        }
    }
}

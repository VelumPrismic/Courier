package me.vel.courier.commands;

import me.vel.courier.Courier;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscordAdminCommand implements CommandExecutor, TabCompleter {

    private final Courier plugin;

    public DiscordAdminCommand(Courier plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("courier.discord.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "link":
                return handleLink(sender, args);
            case "unlink":
                return handleUnlink(sender, args);
            case "check":
                return handleCheck(sender, args);
            case "reload":
                return handleReload(sender);
            case "resetreward":
                return handleResetReward(sender, args);
            case "save":
                return handleSave(sender);
            case "info":
                return handleInfo(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleLink(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cUsage: /discordadmin link <player> <discordId>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cPlayer not found!");
            return true;
        }

        long discordId;
        try {
            discordId = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cInvalid Discord ID!");
            return true;
        }

        if (plugin.getLinkingManager().isLinked(target.getUniqueId())) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cThis player is already linked!");
            return true;
        }

        if (plugin.getLinkingManager().getMinecraftUuid(discordId) != null) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cThis Discord ID is already linked to another account!");
            return true;
        }

        boolean success = plugin.getLinkingManager().linkAccount(target.getUniqueId(), discordId);
        if (success) {
            sender.sendMessage(plugin.getPrefix() + " §aSuccess! §7You have linked §b" + target.getName() + " §7to Discord ID §e" + discordId + ".");
            playSound(sender, Sound.BLOCK_NOTE_BLOCK_PLING);
        } else {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cFailed to link accounts!");
        }

        return true;
    }

    private boolean handleUnlink(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cUsage: /discordadmin unlink <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cPlayer not found!");
            return true;
        }

        if (!plugin.getLinkingManager().isLinked(target.getUniqueId())) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cThis player is not linked!");
            return true;
        }

        Long discordId = plugin.getLinkingManager().getDiscordId(target.getUniqueId());
        boolean success = plugin.getLinkingManager().unlinkAccount(target.getUniqueId());
        
        if (success && discordId != null) {
            plugin.getDiscordBot().removeRoleFromUser(discordId);
            plugin.getMessageUtil().sendUnlinkNotification(target.getUniqueId(), "an admin.");
            sender.sendMessage(plugin.getPrefix() + " §aSuccess! §7You have unlinked §b" + target.getName() + "§7 from Discord ID §e" + discordId + ".");
            playSound(sender, Sound.BLOCK_NOTE_BLOCK_BASS);
        } else {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cFailed to unlink account!");
        }

        return true;
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cUsage: /discordadmin check <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cPlayer not found!");
            return true;
        }

        if (plugin.getLinkingManager().isLinked(target.getUniqueId())) {
            Long discordId = plugin.getLinkingManager().getDiscordId(target.getUniqueId());
            boolean hasReward = plugin.getDataManager().hasReceivedLinkReward(target.getUniqueId());
            long linkTimestamp = plugin.getDataManager().getLinkTimestamp(target.getUniqueId());
            
            String discordUsername = "Unknown";
            if (discordId != null && plugin.getDiscordBot().isEnabled()) {
                discordUsername = plugin.getDiscordBot().getDiscordUsername(discordId);
            }
            
            String linkDate = "Unknown";
            if (linkTimestamp > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
                linkDate = sdf.format(new java.util.Date(linkTimestamp));
            }
            
            sender.sendMessage("§7§m                                    ");
            sender.sendMessage("§5§nDiscord Link Info - " + target.getName());
            sender.sendMessage("");
            sender.sendMessage("§7Status: §aLinked");
            sender.sendMessage("§7Discord ID: §b" + discordId);
            sender.sendMessage("§7Discord Username: §b" + discordUsername);
            sender.sendMessage("§7Linked Date: §e" + linkDate);
            sender.sendMessage("§7Reward Claimed: " + (hasReward ? "§aYes" : "§cNo"));
            sender.sendMessage("§7§m                                    ");
        } else {
            sender.sendMessage("§7§m                                    ");
            sender.sendMessage("§5§nDiscord Link Info - " + target.getName());
            sender.sendMessage("");
            sender.sendMessage("§7Status: §cNot Linked");
            sender.sendMessage("§7§m                                    ");
        }

        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getDataManager().reloadData();
        sender.sendMessage(plugin.getPrefix() + " §aSuccess! §7Configuration reloaded successfully!");
        playSound(sender, Sound.BLOCK_NOTE_BLOCK_BELL);
        return true;
    }

    private boolean handleResetReward(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cUsage: /discordadmin resetreward <player>");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getPrefix() + " §4Error! §cPlayer not found!");
            return true;
        }

        plugin.getDataManager().setReceivedLinkReward(target.getUniqueId(), false);
        sender.sendMessage(plugin.getPrefix() + " §aSuccess! §7Reset link reward status for §b" + target.getName());
        playSound(sender, Sound.BLOCK_NOTE_BLOCK_HARP);
        return true;
    }

    private boolean handleSave(CommandSender sender) {
        plugin.getDataManager().saveLinkedAccounts(plugin.getLinkingManager().getLinkedAccounts());
        plugin.getDataManager().saveData();
        sender.sendMessage(plugin.getPrefix() + "§aSuccess! §7All data saved successfully!");
        playSound(sender, Sound.BLOCK_NOTE_BLOCK_CHIME);
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        int linkedCount = plugin.getLinkingManager().getLinkedAccounts().size();
        int pendingInvites = plugin.getLinkingManager().getRegisteredInviteCount();
        int rewardedCount = plugin.getDataManager().getAllRewardedPlayers().size();

        sender.sendMessage("§7§m                                    ");
        sender.sendMessage("§5§nCourier Statistics");
        sender.sendMessage("");
        sender.sendMessage("§7Linked Accounts: §b" + linkedCount);
        sender.sendMessage("§7Pending Invites: §e" + pendingInvites);
        sender.sendMessage("§7Rewards Claimed: §a" + rewardedCount);
        sender.sendMessage("§7Courier Status: " + (plugin.getDiscordBot().isEnabled() ? "§aOnline" : "§cOffline"));
        sender.sendMessage("§7§m                                    ");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7§m                                    ");
        sender.sendMessage("§5§lDiscord Admin Commands");
        sender.sendMessage("");
        sender.sendMessage("§d/discordadmin link <player> <discordId> §7- Manually link accounts");
        sender.sendMessage("§d/discordadmin unlink <player> §7- Unlink a player");
        sender.sendMessage("§d/discordadmin check <player> §7- Check link status");
        sender.sendMessage("§d/discordadmin reload §7- Reload configuration");
        sender.sendMessage("§d/discordadmin resetreward <player> §7- Reset reward status");
        sender.sendMessage("§d/discordadmin save §7- Manually save data");
        sender.sendMessage("§d/discordadmin info §7- Show plugin statistics");
        sender.sendMessage("§7§m                                    ");
        playSound(sender, Sound.BLOCK_NOTE_BLOCK_CHIME);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("courier.discord.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("link", "unlink", "check", "reload", "resetreward", "save", "info");
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("unlink") || args[0].equalsIgnoreCase("check") || 
                                  args[0].equalsIgnoreCase("resetreward") || args[0].equalsIgnoreCase("link"))) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> players.add(p.getName()));
            return players;
        }

        return new ArrayList<>();
    }

    private void playSound(CommandSender sender, Sound sound) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }
}

package me.vel.courier.discord.discord_events;

import me.vel.courier.Courier;
import me.vel.courier.discord.DiscordBot;
import me.vel.courier.discord.DiscordLinkingManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class GuildMemberJoinListener extends ListenerAdapter {

    private final Courier plugin;
    private final DiscordBot discordBot;
    private final DiscordLinkingManager linkingManager;
    private final Map<String, Integer> inviteUses;
    private final Guild guild;

    public GuildMemberJoinListener(Courier plugin, DiscordBot discordBot, DiscordLinkingManager linkingManager,
                                   Map<String, Integer> inviteUses, Guild guild) {
        this.plugin = plugin;
        this.discordBot = discordBot;
        this.linkingManager = linkingManager;
        this.inviteUses = inviteUses;
        this.guild = guild;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (guild == null || !event.getGuild().equals(guild)) {
            plugin.getLogger().warning("Guild is null or wrong guild, ignoring");
            return;
        }
        
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            event.getGuild().retrieveInvites().queue(currentInvites -> {

                String usedInviteCode = null;
                UUID playerUuid = null;

                for (Invite invite : currentInvites) {
                    String code = invite.getCode();
                    int currentUses = invite.getUses();
                    int cachedUses = inviteUses.getOrDefault(code, 0);
                    if (currentUses > cachedUses) {
                        usedInviteCode = code;
                        playerUuid = linkingManager.getPlayerFromInvite(code);
                        inviteUses.put(code, currentUses);
                        break;
                    }
                }

                if (usedInviteCode == null) {
                    plugin.getLogger().info("Checking for deleted invites...");
                    for (String code : inviteUses.keySet()) {
                        boolean stillExists = currentInvites.stream().anyMatch(inv -> inv.getCode().equals(code));
                        if (!stillExists) {
                            UUID uuid = linkingManager.getPlayerFromInvite(code);
                            if (uuid != null) {
                                usedInviteCode = code;
                                playerUuid = uuid;
                                inviteUses.remove(code);
                                break;
                            }
                        }
                    }
                }
                
                if (usedInviteCode != null && playerUuid != null) {

                    long discordId = event.getUser().getIdLong();
                    String discordUsername = event.getUser().getName();
                    
                    if (linkingManager.isLinked(playerUuid)) {
                        plugin.getLogger().warning("Player " + playerUuid + " is already linked! Skipping.");
                        discordBot.cacheInviteUses();
                        return;
                    }

                    UUID existingPlayer = linkingManager.getMinecraftUuid(discordId);
                    if (existingPlayer != null) {
                        UUID finalPlayerUuid = playerUuid;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player player = Bukkit.getPlayer(finalPlayerUuid);
                            String playerName = player != null ? player.getName() : Bukkit.getOfflinePlayer(finalPlayerUuid).getName();
                            String existingPlayerName = Bukkit.getOfflinePlayer(existingPlayer).getName();

                            discordBot.sendReport(
                                "SYSTEM",
                                playerName,
                                "Discord account '" + discordUsername + "' is already linked to " + existingPlayerName + ". Possible account sharing or multi-accounting."
                            );

                            if (player != null && player.isOnline()) {
                                String prefix = plugin.getPrefix();
                                player.sendMessage("");
                                player.sendMessage(prefix + " §cAccount Link Failed!");
                                player.sendMessage(prefix + " §7Your Discord account is already linked to another Minecraft account.");
                                player.sendMessage(prefix + " §7If this is an error, please contact staff.");
                                player.sendMessage("");
                            }
                        });
                        
                        linkingManager.removeInvite(usedInviteCode);
                        discordBot.cacheInviteUses();
                        return;
                    }

                    boolean success = linkingManager.linkAccount(playerUuid, discordId);
                    
                    if (success) {
                        linkingManager.removeInvite(usedInviteCode);
                        plugin.getDataManager().setLinkTimestamp(playerUuid, System.currentTimeMillis());
                        
                        String roleId = plugin.getConfig().getString("discord_bot.linked_role_id", "");
                        if (!roleId.isEmpty() && !roleId.equals("YOUR_LINKED_ROLE_ID_HERE")) {
                            Role linkedRole = guild.getRoleById(roleId);
                            if (linkedRole != null) {
                                guild.addRoleToMember(event.getMember(), linkedRole).queue();
                            }
                        }
                        
                        UUID finalPlayerUuid = playerUuid;
                        
                        String minecraftUsername = Bukkit.getOfflinePlayer(playerUuid).getName();
                        if (minecraftUsername != null) {
                            guild.modifyNickname(event.getMember(), minecraftUsername).queue(
                                success2 -> plugin.getLogger().info("Changed Discord nickname for " + discordUsername + " to " + minecraftUsername),
                                error -> plugin.getLogger().warning("Failed to change Discord nickname: " + error.getMessage())
                            );
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Player player = Bukkit.getPlayer(finalPlayerUuid);
                            if (player != null && player.isOnline()) {
                                String prefix = plugin.getPrefix();
                                
                                if (!plugin.getDataManager().hasReceivedLinkReward(finalPlayerUuid)) {
                                    discordBot.giveRewards(player);
                                    plugin.getDataManager().setReceivedLinkReward(finalPlayerUuid, true);
                                    
                                    player.sendMessage("");
                                    player.sendMessage(prefix + " §aAccount Linked Successfully!");
                                    player.sendMessage(prefix + " §7Your Minecraft account has been linked to §b" + discordUsername);
                                    player.sendMessage(prefix + " §7You've received a reward for linking your account!");
                                    player.sendMessage("");

                                    for (Player allPlayers : Bukkit.getOnlinePlayers()) {
                                        allPlayers.sendMessage(player.getName() + " §7has joined our discord and linked their account automatically!");
                                        allPlayers.playSound(allPlayers.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                                    }
                                } else {
                                    player.sendMessage("");
                                    player.sendMessage(prefix + " §aAccount Linked Successfully!");
                                    player.sendMessage(prefix + " §7Your Minecraft account has been linked to §b" + discordUsername);
                                    player.sendMessage(prefix + " §7You've already received the link reward.");
                                    player.sendMessage("");
                                }
                            }
                        });
                    }
                }
                discordBot.cacheInviteUses();
            });
        }, 20L);
    }
}

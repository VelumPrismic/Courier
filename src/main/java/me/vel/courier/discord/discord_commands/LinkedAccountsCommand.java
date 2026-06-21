package me.vel.courier.discord.discord_commands;

import me.vel.courier.Courier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.util.Map;
import java.util.UUID;

public class LinkedAccountsCommand {

    private final Courier plugin;

    public LinkedAccountsCommand(Courier plugin) {
        this.plugin = plugin;
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        String subcommand = event.getSubcommandName();
        
        if (subcommand == null) {
            event.reply("Invalid subcommand!").setEphemeral(true).queue();
            return;
        }
        
        switch (subcommand) {
            case "list":
                handleList(event);
                break;
            case "check":
                handleCheck(event);
                break;
            default:
                event.reply("Unknown subcommand!").setEphemeral(true).queue();
                break;
        }
    }

    private void handleList(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            Map<UUID, Long> linkedAccounts = plugin.getLinkingManager().getLinkedAccounts();
            
            if (linkedAccounts.isEmpty()) {
                event.getHook().sendMessage("No linked accounts found!").setEphemeral(true).queue();
                return;
            }
            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);
            embed.setTitle("🔗 Linked Accounts");
            embed.setDescription("Total linked accounts: **" + linkedAccounts.size() + "**");
            
            StringBuilder accountsList = new StringBuilder();
            int count = 0;
            
            for (Map.Entry<UUID, Long> entry : linkedAccounts.entrySet()) {
                UUID playerUuid = entry.getKey();
                Long discordId = entry.getValue();
                
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
                String playerName = player.getName() != null ? player.getName() : "Unknown";
                String discordUsername = plugin.getDiscordBot().getDiscordUsername(discordId);
                
                accountsList.append("**").append(playerName).append("** → ").append(discordUsername).append("\n");
                count++;
                
                if (count >= 25) {
                    accountsList.append("*...and ").append(linkedAccounts.size() - 25).append(" more*");
                    break;
                }
            }
            
            embed.addField("Linked Accounts", accountsList.toString(), false);
            embed.setFooter(plugin.getServerName() + " Discord Link System");
            embed.setTimestamp(java.time.Instant.now());
            
            event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
        });
    }

    private void handleCheck(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        
        OptionMapping playerOption = event.getOption("player");
        if (playerOption == null) {
            event.getHook().sendMessage("Please provide a player name!").setEphemeral(true).queue();
            return;
        }
        
        String playerName = playerOption.getAsString();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                event.getHook().sendMessage("Player not found!").setEphemeral(true).queue();
                return;
            }
            
            UUID playerUuid = target.getUniqueId();
            boolean isLinked = plugin.getLinkingManager().isLinked(playerUuid);
            
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("🔍 Link Status Check");
            
            String actualName = target.getName() != null ? target.getName() : playerName;
            
            if (isLinked) {
                Long discordId = plugin.getLinkingManager().getDiscordId(playerUuid);
                String discordUsername = discordId != null ? plugin.getDiscordBot().getDiscordUsername(discordId) : "Unknown";
                
                embed.setColor(Color.GREEN);
                embed.setDescription("✅ **" + actualName + "** is linked to Discord!");
                embed.addField("Discord Account", discordUsername, false);
                embed.addField("Discord ID", String.valueOf(discordId), false);
            } else {
                embed.setColor(Color.RED);
                embed.setDescription("❌ **" + actualName + "** is not linked to Discord!");
            }
            
            embed.setFooter("UUID: " + playerUuid.toString());
            embed.setTimestamp(java.time.Instant.now());
            
            event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
        });
    }
}

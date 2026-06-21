package me.vel.courier.discord.discord_commands;

import me.vel.courier.Courier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LinkHistoryCommand {

    private final Courier plugin;

    public LinkHistoryCommand(Courier plugin) {
        this.plugin = plugin;
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.MODERATE_MEMBERS)) {
            event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

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
            List<Map<String, Object>> history = plugin.getDataManager().getLinkHistory(playerUuid);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);
            embed.setTitle("📜 Link History - " + target.getName());

            if (history.isEmpty()) {
                embed.setDescription("No link history found for this player.");
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
                StringBuilder historyText = new StringBuilder();

                for (int i = 0; i < history.size(); i++) {
                    Map<String, Object> entry = history.get(i);
                    long discordId = (long) entry.get("discord_id");
                    long timestamp = (long) entry.get("timestamp");
                    String action = (String) entry.get("action");

                    String date = sdf.format(new Date(timestamp));
                    String discordUsername = plugin.getDiscordBot().getDiscordUsername(discordId);

                    historyText.append("**").append(i + 1).append(".** ")
                            .append(action).append("\n")
                            .append("└ Discord: ").append(discordUsername).append(" (").append(discordId).append(")\n")
                            .append("└ Date: ").append(date).append("\n\n");
                }

                embed.setDescription(historyText.toString());
            }

            boolean currentlyLinked = plugin.getLinkingManager().isLinked(playerUuid);
            if (currentlyLinked) {
                Long currentDiscordId = plugin.getLinkingManager().getDiscordId(playerUuid);
                long linkTimestamp = plugin.getDataManager().getLinkTimestamp(playerUuid);
                String linkDate = linkTimestamp > 0 ? 
                        new SimpleDateFormat("MMM dd, yyyy HH:mm").format(new Date(linkTimestamp)) : "Unknown";

                embed.addField("Current Status", "✅ Linked", true);
                embed.addField("Discord ID", String.valueOf(currentDiscordId), true);
                embed.addField("Linked Since", linkDate, true);
            } else {
                embed.addField("Current Status", "Not Linked", false);
            }

            embed.setFooter("UUID: " + playerUuid.toString());
            embed.setTimestamp(java.time.Instant.now());

            event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
        });
    }
}

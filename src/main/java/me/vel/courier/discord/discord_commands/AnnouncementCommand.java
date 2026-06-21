package me.vel.courier.discord.discord_commands;

import me.vel.courier.Courier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;

public class AnnouncementCommand {

    private final Courier plugin;

    public AnnouncementCommand(Courier plugin) {
        this.plugin = plugin;
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        TextInput title = TextInput.create("title", "Announcement Title", TextInputStyle.SHORT)
                .setPlaceholder("Enter the announcement title")
                .setRequired(true)
                .setMaxLength(100)
                .build();

        TextInput message = TextInput.create("message", "Announcement Message", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Enter the announcement message")
                .setRequired(true)
                .setMaxLength(2000)
                .build();

        TextInput color = TextInput.create("color", "Embed Color (hex)", TextInputStyle.SHORT)
                .setPlaceholder("e.g., #FF5733 or leave empty for default")
                .setRequired(false)
                .setMaxLength(7)
                .build();

        TextInput channelId = TextInput.create("channel_id", "Channel ID", TextInputStyle.SHORT)
                .setPlaceholder("Enter channel ID or leave empty for default")
                .setRequired(false)
                .setMaxLength(20)
                .build();

        TextInput roleId = TextInput.create("role_id", "Role ID to Ping", TextInputStyle.SHORT)
                .setPlaceholder("Enter role ID, 'everyone', or leave empty")
                .setRequired(false)
                .setMaxLength(20)
                .build();

        Modal modal = Modal.create("announcement_modal", "Create Announcement")
                .addComponents(
                        ActionRow.of(title),
                        ActionRow.of(message),
                        ActionRow.of(color),
                        ActionRow.of(channelId),
                        ActionRow.of(roleId)
                )
                .build();

        event.replyModal(modal).queue();
    }

    public void handleModal(ModalInteractionEvent event) {
        String title = event.getValue("title").getAsString();
        String message = event.getValue("message").getAsString();
        String colorHex = event.getValue("color") != null ? event.getValue("color").getAsString() : null;
        String channelIdInput = event.getValue("channel_id") != null ? event.getValue("channel_id").getAsString() : null;
        String roleIdInput = event.getValue("role_id") != null ? event.getValue("role_id").getAsString() : null;

        Color embedColor = Color.BLUE;
        if (colorHex != null && !colorHex.isEmpty()) {
            try {
                embedColor = Color.decode(colorHex);
            } catch (NumberFormatException e) {
//                event.reply("Invalid color format! Using default blue.").setEphemeral(true).queue();
            }
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📢 " + title);
        embed.setDescription(message);
        embed.setColor(embedColor);
        embed.setFooter("Announced by " + event.getUser().getName());
        embed.setTimestamp(java.time.Instant.now());

        String channelId = (channelIdInput != null && !channelIdInput.isEmpty()) ? 
            channelIdInput : plugin.getConfig().getString("discord_bot.channels.minecraft_chat", "");
        
        if (channelId.isEmpty()) {
            event.reply("No channel specified and no default configured!").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getGuild().getTextChannelById(channelId);
        if (channel == null) {
            event.reply("Channel not found!").setEphemeral(true).queue();
            return;
        }

        String mention = "";
        if (roleIdInput != null && !roleIdInput.isEmpty()) {
            if (roleIdInput.equalsIgnoreCase("everyone")) {
                mention = "@everyone ";
            } else {
                Role role = event.getGuild().getRoleById(roleIdInput);
                if (role != null) {
                    mention = role.getAsMention() + " ";
                }
            }
        }

        String finalMention = mention;
        channel.sendMessage(finalMention).setEmbeds(embed.build()).queue(
                success -> event.reply("Announcement successfully sent!").setEphemeral(true).queue(),
                error -> event.reply("Failed to send announcement: " + error.getMessage()).setEphemeral(true).queue()
        );
    }
}

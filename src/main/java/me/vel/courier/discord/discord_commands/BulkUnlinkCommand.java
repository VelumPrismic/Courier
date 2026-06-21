package me.vel.courier.discord.discord_commands;

import me.vel.courier.Courier;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.Map;
import java.util.UUID;

public class BulkUnlinkCommand {

    private final Courier plugin;

    public BulkUnlinkCommand(Courier plugin) {
        this.plugin = plugin;
    }

    public void handle(SlashCommandInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("You don't have permission to use this command!").setEphemeral(true).queue();
            return;
        }

        int linkedCount = plugin.getLinkingManager().getLinkedAccounts().size();

        event.reply("**WARNING: Bulk Unlink Operation**\n\n" +
                "This will unlink **" + linkedCount + " accounts**.\n" +
                "This action cannot be undone!\n\n" +
                "Are you sure you want to proceed?")
                .addActionRow(
                        Button.danger("bulkunlink_confirm", "Confirm Unlink"),
                        Button.secondary("bulkunlink_cancel", "Cancel")
                )
                .setEphemeral(true)
                .queue();
    }

    public void handleButton(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (buttonId.equals("bulkunlink_cancel")) {
            event.editMessage("Bulk unlink operation cancelled.")
                    .setComponents()
                    .queue();
            return;
        }

        if (buttonId.equals("bulkunlink_confirm")) {
            if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("You don't have permission to do this!").setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();

            Map<UUID, Long> linkedAccounts = plugin.getLinkingManager().getLinkedAccounts();
            int count = linkedAccounts.size();

            for (Map.Entry<UUID, Long> entry : linkedAccounts.entrySet()) {
                plugin.getLinkingManager().unlinkAccount(entry.getKey());
                plugin.getDiscordBot().removeRoleFromUser(entry.getValue());
                plugin.getMessageUtil().sendUnlinkNotification(entry.getKey(), "a bulk unlink.");
                plugin.getDataManager().addLinkHistory(entry.getKey(), entry.getValue(), System.currentTimeMillis(), "BULK_UNLINK");
            }

            event.getHook().editOriginal("✅ Successfully unlinked **" + count + " accounts**.")
                    .setComponents()
                    .queue();

            plugin.getLogger().info("[BulkUnlink] " + event.getUser().getName() + " unlinked " + count + " accounts");
        }
    }
}

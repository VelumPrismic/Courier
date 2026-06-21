package me.vel.courier.discord.discord_events;

import me.vel.courier.Courier;
import me.vel.courier.discord.discord_commands.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SlashCommandListener extends ListenerAdapter {

    private final Courier plugin;

    private final ServerStatsCommand serverStatsCommand;
    private final AnnouncementCommand announcementCommand;
    private final BulkUnlinkCommand bulkUnlinkCommand;
    private final LinkHistoryCommand linkHistoryCommand;
    private final LinkedAccountsCommand linkedAccountsCommand;

    public SlashCommandListener(Courier plugin) {
        this.plugin = plugin;
        this.serverStatsCommand = new ServerStatsCommand(plugin);
        this.announcementCommand = new AnnouncementCommand(plugin);
        this.bulkUnlinkCommand = new BulkUnlinkCommand(plugin);
        this.linkHistoryCommand = new LinkHistoryCommand(plugin);
        this.linkedAccountsCommand = new LinkedAccountsCommand(plugin);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        
        switch (commandName) {
            case "serverstats":
                serverStatsCommand.handle(event);
                break;
            case "announce":
                announcementCommand.handle(event);
                break;
            case "bulkunlink":
                bulkUnlinkCommand.handle(event);
                break;
            case "linkhistory":
                linkHistoryCommand.handle(event);
                break;
            case "linkedaccounts":
                linkedAccountsCommand.handle(event);
                break;
            default:
                event.reply("Unknown command!").setEphemeral(true).queue();
                break;
        }
    }
}

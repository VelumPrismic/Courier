package me.vel.courier.discord.discord_events;

import me.vel.courier.discord.DiscordBot;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class StringSelectListener extends ListenerAdapter {

    private final DiscordBot discordBot;

    public StringSelectListener(DiscordBot discordBot) {
        this.discordBot = discordBot;
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
    }
}
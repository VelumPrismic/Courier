package me.vel.courier.discord.discord_events;

import me.vel.courier.Courier;
import me.vel.courier.discord.DiscordLinkingManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import java.util.UUID;

public class GuildMemberRemoveListener extends ListenerAdapter {

    private final Courier plugin;
    private final DiscordLinkingManager linkingManager;
    private final Guild guild;

    public GuildMemberRemoveListener(Courier plugin, DiscordLinkingManager linkingManager, Guild guild) {
        this.plugin = plugin;
        this.linkingManager = linkingManager;
        this.guild = guild;
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (guild == null || !event.getGuild().equals(guild)) {
            return;
        }
        
        long discordId = event.getUser().getIdLong();
        
        UUID playerUuid = linkingManager.getMinecraftUuid(discordId);
        
        if (playerUuid != null) {
            boolean success = linkingManager.unlinkDiscordAccount(discordId);
            
            if (success) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getMessageUtil().sendUnlinkNotification(playerUuid, "you leaving/being kicked from the discord server, you can relink your account using /discord.");
                });
            }
        }
    }
}

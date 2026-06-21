package me.vel.courier.discord.discord_commands;

import me.vel.courier.Courier;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.text.DecimalFormat;
import java.util.Collection;

public class ServerStatsCommand {

    private final Courier plugin;
    private final DecimalFormat df = new DecimalFormat("#.##");

    public ServerStatsCommand(Courier plugin) {
        this.plugin = plugin;
    }

    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);
            embed.setTitle("📊 Server Statistics");
            
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            int onlineCount = players.size();
            int maxPlayers = Bukkit.getMaxPlayers();
            
            double tps = getTPS();
            String tpsColor = getTpsEmoji(tps);
            
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1048576;
            long maxMemory = runtime.maxMemory() / 1048576;
            double memoryPercent = (double) usedMemory / maxMemory * 100;
            
            long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
            String uptime = formatUptime(uptimeMillis);
            
            String bukkitVersion = Bukkit.getBukkitVersion();
            
            embed.addField("👥 Players", onlineCount + "/" + maxPlayers, true);
            embed.addField("⚡ TPS", tpsColor + " " + df.format(tps), true);
            embed.addField("💾 Memory", usedMemory + "MB / " + maxMemory + "MB (" + df.format(memoryPercent) + "%)", true);
            
            embed.addField("⏰ Uptime", uptime, true);
            embed.addField("📦 Version", bukkitVersion, true);
            embed.addField("\u200B", "\u200B", true);
            
            if (onlineCount > 0) {
                StringBuilder playerList = new StringBuilder();
                int count = 0;
                for (Player p : players) {
                    if (count >= 10) {
                        playerList.append("\n...and ").append(onlineCount - 10).append(" more");
                        break;
                    }
                    playerList.append(p.getName()).append("\n");
                    count++;
                }
                embed.addField("👤 Online Players", playerList.toString(), false);
            }
            
            embed.setFooter(plugin.getServerName());
            embed.setTimestamp(java.time.Instant.now());
            
            event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
        });
    }

    private double getTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            double[] tps = (double[]) server.getClass().getField("recentTps").get(server);
            return Math.min(tps[0], 20.0);
        } catch (Exception e) {
            return 20.0;
        }
    }

    private String getTpsEmoji(double tps) {
        if (tps >= 19.0) return "🟢";
        if (tps >= 17.0) return "🟡";
        return "🔴";
    }

    private String formatUptime(long uptimeMillis) {
        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}

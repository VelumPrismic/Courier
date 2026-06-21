package me.vel.courier.utils;

import me.vel.courier.Courier;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeManager {

    private final Courier plugin;
    private ZoneId timezone;
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter dateTimeFormatter;

    public TimeManager(Courier plugin) {
        this.plugin = plugin;
        loadTimezone();
    }

    public void loadTimezone() {
        String timezoneString = plugin.getConfig().getString("discord_bot.timezone", "UTC");
        try {
            this.timezone = ZoneId.of(timezoneString);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid timezone '" + timezoneString + "', using UTC");
            this.timezone = ZoneId.of("UTC");
        }
        this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(timezone);
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm").withZone(timezone);
    }

    public String getCurrentTimestamp() {
        return timeFormatter.format(Instant.now());
    }

    public String formatDateTime(long timestamp) {
        return dateTimeFormatter.format(Instant.ofEpochMilli(timestamp));
    }

    public String formatTime(long timestamp) {
        return timeFormatter.format(Instant.ofEpochMilli(timestamp));
    }

    public ZoneId getTimezone() {
        return timezone;
    }
}

package me.vel.courier;

import me.vel.courier.api.DiscordAPI;
import me.vel.courier.commands.DiscordAdminCommand;
import me.vel.courier.commands.DiscordCommand;
import me.vel.courier.discord.DiscordBot;
import me.vel.courier.discord.DiscordChatListener;
import me.vel.courier.discord.DiscordLinkingManager;
import me.vel.courier.listeners.CommandListener;
import me.vel.courier.listeners.PlayerJoinListener;
import me.vel.courier.manager.DataManager;
import me.vel.courier.utils.MessageUtil;
import me.vel.courier.utils.TimeManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Courier extends JavaPlugin {

    private DiscordBot discordBot;
    private DiscordAPI discordAPI;
    private DiscordLinkingManager linkingManager;
    private DataManager dataManager;
    private MessageUtil messageUtil;
    private TimeManager timeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new DataManager(this);
        linkingManager = new DiscordLinkingManager();
        linkingManager.setPlugin(this);
        messageUtil = new MessageUtil(this);
        timeManager = new TimeManager(this);
        discordBot = new DiscordBot(this, linkingManager);
        discordAPI = new DiscordAPI(this, discordBot);

        getServer().getPluginManager().registerEvents(new DiscordChatListener(this, discordBot), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);

        DiscordAdminCommand adminCommand = new DiscordAdminCommand(this);
        getCommand("discord").setExecutor(new DiscordCommand(this));
        getCommand("discordadmin").setExecutor(adminCommand);
        getCommand("discordadmin").setTabCompleter(adminCommand);

        discordBot.startBot();

        getLogger().info("Courier has been enabled!");
    }

    @Override
    public void onDisable() {
        if (discordBot != null) {
            discordBot.stopBot();
        }

        if (dataManager != null) {
            dataManager.close();
        }

        getLogger().info("Courier has been disabled!");
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public DiscordAPI getDiscordAPI() {
        return discordAPI;
    }

    public DiscordLinkingManager getLinkingManager() {
        return linkingManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public String getPrefix() {
        return getConfig().getString("discord_prefix", "&8&l| &5❄ &5&lDISCORD &8&l→").replace("&", "§");
    }

    public String getServerName() {
        return getConfig().getString("server_name", "MyServer");
    }

    public String getServerIp() {
        return getConfig().getString("server_ip", "play.myserver.com");
    }
}

# Courier

A Discord-Minecraft bridge plugin that links Discord servers with Minecraft servers. Players can link their Discord and Minecraft accounts, relay chat between platforms, and use Discord slash commands to interact with the server.

## Technologies Used

- `Java 21`
- `Paper API 1.21.11`
- `JDA 5.3.0`
- `SQLite`

## Features

Here's what you can do with this plugin:

- **Discord Linking System**: Link Minecraft accounts to Discord accounts through unique single-use invite codes. Players receive a linked role and configurable rewards upon linking. Staff can manage links through Discord admin commands or in-game commands.

- **Chat Relay**: Bidirectional chat between Minecraft and Discord. Messages sent in-game appear in the configured Discord channel and vice versa, with customizable formatting and timestamps.

- **Console Relay**: Execute server commands directly from a Discord channel. All command output is relayed back, and in-game commands can be logged to the same channel.

- **Chat Filtering**: Block unwanted words and regex patterns in Minecraft chat. Filtered messages are logged to a designated staff channel for review.

- **Slash Commands**: Discord slash commands for server statistics (`/serverstats`), announcements (`/announce`), bulk unlinking (`/bulkunlink`), link history (`/linkhistory`), reports (`/reports`), and linked account management (`/linkedaccounts`). Admin commands are hidden from normal members.

- **Player Tracking**: Server online/offline status messages, player join and leave notifications, and invite tracking with cooldowns.

## What I Learned

### Discord Bot Architecture

- **JDA Event System**: Handling Discord events through JDA's `ListenerAdapter` for slash commands, modals, button interactions, string selects, and guild member events.
- **Slash Commands**: Registering guild-specific slash commands with permission defaults to restrict visibility based on roles.
- **Modals & Components**: Building interactive forms with modals, dropdown menus, and buttons for announcement creation and reporting flows.

### Database with SQLite

- **JDBC & SQLite**: Using raw JDBC with the SQLite driver for lightweight, file-based persistence without requiring a separate database server.
- **Schema Migration**: Auto-creating tables on startup and migrating existing YAML data to SQLite on first run.

### Plugin Architecture

- **Bukkit API Integration**: Bridging the Minecraft server thread with Discord's async event system using `Bukkit.getScheduler().runTask()` for thread-safe operations.
- **Config-Driven Design**: Externalizing all bot behavior (channels, roles, formatting, rewards) into `config.yml` so server admins can customize without code changes.
- **API Layer**: Providing a `DiscordAPI` class that other plugins can use to send messages, embeds, and interact with the bot programmatically.

## Running the Project

To run the project in your local environment, follow these steps:

1. Clone the repository to your local machine.
2. Build the plugin with Maven:
   ```
   mvn clean package
   ```
3. Place the generated JAR from `target/` into your server's `plugins/` folder.
4. Configure `config.yml` with your Discord bot token, guild ID, and channel IDs.
5. Restart your server to enable the plugin.

package org.hubreturn.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.hubreturn.core.CommandActor;
import org.hubreturn.core.HubCore;
import org.hubreturn.core.Platform;
import org.hubreturn.core.PlayerRef;
import org.hubreturn.core.ServerRef;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class BungeeBootstrap extends Plugin implements Platform {

    private final CommandActor console = new ConsoleActor();

    private HubCore core;
    private String channel;

    @Override
    public void onEnable() {
        try {
            core = HubCore.start(this);
        } catch (Exception bad) {
            getLogger().log(Level.SEVERE, bad.toString(), bad);
            return;
        }
        getProxy().getPluginManager().registerCommand(this, new HubCommand(core, this));
        getProxy().getPluginManager().registerListener(this, new BungeeListeners(core));
    }

    @Override
    public void onDisable() {
        if (core != null) {
            core.shutdown();
        }
    }

    @Override
    public Path dataDirectory() {
        return getDataFolder().toPath();
    }

    @Override
    public String platformName() {
        return "BungeeCord";
    }

    @Override
    public Optional<PlayerRef> player(String name) {
        return Optional.ofNullable(getProxy().getPlayer(name)).map(BungeePlayer::new);
    }

    @Override
    public Optional<ServerRef> server(String name) {
        return Optional.ofNullable(getProxy().getServerInfo(name)).map(BungeeServer::new);
    }

    @Override
    public CommandActor console() {
        return console;
    }

    @Override
    public void registerChannel(String name) {
        getProxy().registerChannel(name);
        channel = name;
    }

    @Override
    public void unregisterChannel(String name) {
        if (channel != null) {
            getProxy().unregisterChannel(channel);
            channel = null;
        }
    }

    @Override
    public void sendToServer(String serverName, String name, byte[] data) {
        ServerInfo info = getProxy().getServerInfo(serverName);
        if (info != null) {
            info.sendData(name, data);
        }
    }

    @Override
    public void schedule(Runnable task, long delayMillis) {
        getProxy().getScheduler().schedule(this, task, delayMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void info(String text) {
        getLogger().info(text);
    }

    @Override
    public void warn(String text) {
        getLogger().warning(text);
    }

    @Override
    public void error(String text, Throwable error) {
        getLogger().log(Level.SEVERE, text, error);
    }

    private final class ConsoleActor implements CommandActor {
        @Override
        public String name() {
            return "CONSOLE";
        }

        @Override
        public boolean isPlayer() {
            return false;
        }

        @Override
        public boolean hasPermission(String node) {
            return true;
        }

        @Override
        public void sendMessage(String legacy) {
            getProxy().getConsole().sendMessage(TextComponent.fromLegacyText(
                    ChatColor.translateAlternateColorCodes('&', legacy)));
        }

        @Override
        public void sendActionMessage(String template, String placeholder, String label, String command) {
            sendMessage(template.replace(placeholder, label));
        }
    }
}

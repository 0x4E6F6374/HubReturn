package org.hubreturn.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.hubreturn.core.CommandActor;
import org.hubreturn.core.HubCore;
import org.hubreturn.core.Platform;
import org.hubreturn.core.PlayerRef;
import org.hubreturn.core.ServerRef;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class VelocityBootstrap implements Platform {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDir;
    private final CommandActor console = new ConsoleActor();

    private HubCore core;
    private ChannelIdentifier channelId;
    private CommandMeta commandMeta;

    @Inject
    public VelocityBootstrap(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        try {
            core = HubCore.start(this);
        } catch (Exception bad) {
            logger.error(bad.toString(), bad);
            throw new IllegalStateException(bad);
        }
        proxy.getEventManager().register(this, new VelocityListeners(core));
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (core != null) {
            core.shutdown();
        }
    }

    @Override
    public Path dataDirectory() {
        return dataDir;
    }

    @Override
    public String platformName() {
        return "Velocity";
    }

    @Override
    public Optional<PlayerRef> player(String name) {
        return proxy.getPlayer(name).map(VelocityPlayer::new);
    }

    @Override
    public Optional<ServerRef> server(String name) {
        return proxy.getServer(name).map(VelocityServer::new);
    }

    @Override
    public CommandActor console() {
        return console;
    }

    @Override
    public void registerCommands(HubCore current) {
        if (commandMeta != null) {
            proxy.getCommandManager().unregister(commandMeta);
        }
        commandMeta = proxy.getCommandManager().metaBuilder(current.settings().commandName())
                .aliases(current.settings().commandAliases().toArray(new String[0]))
                .plugin(this)
                .build();
        proxy.getCommandManager().register(commandMeta, new HubCommand(current, this));
    }

    @Override
    public void registerChannel(String channel) {
        ChannelIdentifier id = MinecraftChannelIdentifier.from(channel);
        proxy.getChannelRegistrar().register(id);
        channelId = id;
    }

    @Override
    public void unregisterChannel(String channel) {
        if (channelId != null) {
            proxy.getChannelRegistrar().unregister(channelId);
            channelId = null;
        }
    }

    @Override
    public void sendToServer(String serverName, String channel, byte[] data) {
        if (channelId != null) {
            proxy.getServer(serverName).ifPresent(server -> server.sendPluginMessage(channelId, data));
        }
    }

    @Override
    public void schedule(Runnable task, long delayMillis) {
        proxy.getScheduler().buildTask(this, task)
                .delay(delayMillis, TimeUnit.MILLISECONDS)
                .schedule();
    }

    @Override
    public void info(String text) {
        logger.info(text);
    }

    @Override
    public void warn(String text) {
        logger.warn(text);
    }

    @Override
    public void error(String text, Throwable error) {
        logger.error(text, error);
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
        public Locale locale() {
            return null;
        }

        @Override
        public void sendMessage(String legacy) {
            proxy.getConsoleCommandSource().sendMessage(
                    LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
        }

        @Override
        public void sendActionMessage(String template, String placeholder, String label, String command) {
            sendMessage(template.replace(placeholder, label));
        }
    }
}

package org.hubreturn.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.event.ServerKickEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.hubreturn.core.HubCore;
import org.hubreturn.core.ServerRef;
import org.hubreturn.core.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BungeeListeners implements Listener {

    private final HubCore core;

    BungeeListeners(HubCore core) {
        this.core = core;
    }

    @EventHandler
    public void onKick(ServerKickEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String from = event.getKickedFrom().getName();
        BaseComponent original = event.getReason();
        String reason = original == null ? "" : BaseComponent.toLegacyText(original);

        core.onKick(new HubCore.KickView() {
            @Override
            public String playerName() {
                return player.getName();
            }

            @Override
            public String fromServer() {
                return from;
            }

            @Override
            public String reason() {
                return reason;
            }

            @Override
            public boolean hasPermission(String node) {
                return player.hasPermission(node);
            }

            @Override
            public void redirectTo(ServerRef target, String message) {
                // 新版 BungeeCord 把踢出原因收成单个 BaseComponent，接管后的提示也复用这个字段
                event.setCancelled(true);
                event.setCancelServer(((BungeeServer) target).raw());
                event.setReason(TextComponent.fromLegacy(
                        ChatColor.translateAlternateColorCodes('&', message)));
            }
        });
    }

    @EventHandler
    public void onConnect(ServerConnectEvent event) {
        if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) {
            return;
        }
        core.pickInitial(new BungeePlayer(event.getPlayer()))
                .ifPresent(server -> event.setTarget(((BungeeServer) server).raw()));
    }

    @EventHandler
    public void onConnected(ServerConnectedEvent event) {
        core.onServerConnected(event.getPlayer().getUniqueId(),
                event.getServer().getInfo().getName());
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer) || event.isCommand()) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        String replacement = core.onChat(player.getUniqueId(), player.getName(), event.getMessage());
        if (replacement != null) {
            event.setMessage(replacement);
        }
    }

    @EventHandler
    public void onMessage(PluginMessageEvent event) {
        String channel = event.getTag();
        if (!channel.equals(core.settings().channel())) {
            return;
        }
        event.setCancelled(true);

        Connection sender = event.getSender();
        boolean fromPlayer = sender instanceof ProxiedPlayer;
        String source = sender instanceof Server ? ((Server) sender).getInfo().getName() : "";
        core.onPluginMessage(channel, source, fromPlayer, event.getData());
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        String cursor = event.getCursor();
        String text = cursor.startsWith("/") ? cursor.substring(1) : cursor;
        String[] parts = text.split(" ", 2);
        Settings current = core.settings();
        if (!commandMatches(parts[0], current) || (parts.length > 1 && parts[1].contains(" "))) {
            return;
        }
        String prefix = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "";
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        List<String> out = new ArrayList<>();
        add(out, prefix, current.reloadSubcommand(), permitted(player, current.reloadPermission()));
        add(out, prefix, current.confirmSubcommand(), permitted(player, current.commandPermission()));
        add(out, prefix, current.confirmCancelSubcommand(), permitted(player, current.commandPermission()));
        event.getSuggestions().addAll(out);
    }

    private static boolean commandMatches(String typed, Settings current) {
        if (typed.equalsIgnoreCase(current.commandName())) {
            return true;
        }
        for (String alias : current.commandAliases()) {
            if (typed.equalsIgnoreCase(alias)) {
                return true;
            }
        }
        return false;
    }

    private static void add(List<String> out, String prefix, String value, boolean permitted) {
        if (permitted && value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            out.add(value);
        }
    }

    private static boolean permitted(ProxiedPlayer player, String node) {
        return node.isEmpty() || player.hasPermission(node);
    }
}

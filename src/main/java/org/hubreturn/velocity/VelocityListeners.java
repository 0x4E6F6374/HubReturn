package org.hubreturn.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.hubreturn.core.HubCore;
import org.hubreturn.core.ServerRef;

import java.util.Locale;

final class VelocityListeners {

    private final HubCore core;

    VelocityListeners(HubCore core) {
        this.core = core;
    }

    @Subscribe
    public void onKick(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        String from = event.getServer().getServerInfo().getName();
        String reason = event.getServerKickReason()
                .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                .orElse("");

        core.onKick(new HubCore.KickView() {
            @Override
            public String playerName() {
                return player.getUsername();
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
            public Locale locale() {
                return player.hasSentPlayerSettings() ? player.getEffectiveLocale() : null;
            }

            @Override
            public void redirectTo(ServerRef target, String message) {
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(
                        ((VelocityServer) target).raw(),
                        LegacyComponentSerializer.legacyAmpersand().deserialize(message)));
            }
        });
    }

    @Subscribe
    public void onChoose(PlayerChooseInitialServerEvent event) {
        core.pickInitial(new VelocityPlayer(event.getPlayer()))
                .ifPresent(server -> event.setInitialServer(((VelocityServer) server).raw()));
    }

    @Subscribe
    public void onPostConnect(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        String server = player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("");
        core.onServerConnected(player.getUniqueId(), server);
    }

    @Subscribe
    @SuppressWarnings("deprecation")
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        String replacement = core.onChat(player.getUniqueId(), player.getUsername(), event.getMessage());
        if (replacement != null) {
            // 1.19.1 起把聊天结果设成 denied 会把玩家踢下线，这里只替换文本内容
            event.setResult(PlayerChatEvent.ChatResult.message(replacement));
        }
    }

    @Subscribe
    public void onMessage(PluginMessageEvent event) {
        String channel = event.getIdentifier().getId();
        if (!channel.equals(core.settings().channel())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        boolean fromPlayer = event.getSource() instanceof Player;
        String source = "";
        if (event.getSource() instanceof ServerConnection) {
            source = ((ServerConnection) event.getSource()).getServerInfo().getName();
        }
        core.onPluginMessage(channel, source, fromPlayer, event.getData());
    }
}

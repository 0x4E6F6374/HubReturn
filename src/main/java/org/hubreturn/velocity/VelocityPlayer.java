package org.hubreturn.velocity;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.hubreturn.core.PlayerRef;
import org.hubreturn.core.ServerRef;

import java.util.UUID;

final class VelocityPlayer implements PlayerRef {

    private final Player player;

    VelocityPlayer(Player player) {
        this.player = player;
    }

    @Override
    public String name() {
        return player.getUsername();
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    @Override
    public boolean hasPermission(String node) {
        return player.hasPermission(node);
    }

    @Override
    public UUID uuid() {
        return player.getUniqueId();
    }

    @Override
    public String currentServer() {
        return player.getCurrentServer()
                .map(connection -> connection.getServerInfo().getName())
                .orElse("");
    }

    @Override
    public void sendMessage(String legacy) {
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(legacy));
    }

    @Override
    public void sendActionMessage(String template, String placeholder, String label, String command) {
        int at = template.indexOf(placeholder);
        if (at < 0) {
            sendMessage(template);
            return;
        }
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        Component head = legacy.deserialize(template.substring(0, at));
        Component tail = legacy.deserialize(template.substring(at + placeholder.length()));
        Component button = legacy.deserialize(label).clickEvent(ClickEvent.runCommand(command));
        player.sendMessage(head.append(button).append(tail));
    }

    @Override
    public void connect(ServerRef target, ConnectCallback callback) {
        player.createConnectionRequest(((VelocityServer) target).raw()).connect()
                .whenComplete((result, error) -> {
                    if (error != null) {
                        callback.done(false, error.toString());
                        return;
                    }
                    String detail = result.getReasonComponent()
                            .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                            .orElse("");
                    callback.done(result.isSuccessful(), detail);
                });
    }
}

package org.hubreturn.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.hubreturn.core.PlayerRef;
import org.hubreturn.core.ServerRef;

import java.util.UUID;

final class BungeePlayer implements PlayerRef {

    private final ProxiedPlayer player;

    BungeePlayer(ProxiedPlayer player) {
        this.player = player;
    }

    @Override
    public String name() {
        return player.getName();
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
        return player.getServer() == null ? "" : player.getServer().getInfo().getName();
    }

    @Override
    public void sendMessage(String legacy) {
        player.sendMessage(TextComponent.fromLegacyText(
                ChatColor.translateAlternateColorCodes('&', legacy)));
    }

    @Override
    public void sendActionMessage(String template, String placeholder, String label, String command) {
        int at = template.indexOf(placeholder);
        if (at < 0) {
            sendMessage(template);
            return;
        }
        TextComponent root = new TextComponent();
        append(root, template.substring(0, at), null);
        append(root, label, command);
        append(root, template.substring(at + placeholder.length()), null);
        player.sendMessage(root);
    }

    @Override
    public void connect(ServerRef target, ConnectCallback callback) {
        ServerInfo info = ((BungeeServer) target).raw();
        player.connect(info, (result, error) -> {
            if (error != null) {
                callback.done(false, error.toString());
                return;
            }
            callback.done(Boolean.TRUE.equals(result), "");
        });
    }

    private static void append(TextComponent root, String legacy, String command) {
        // 点击事件挂在每一个分段上，避免外层容器的样式没有继承到子文本
        for (BaseComponent part : TextComponent.fromLegacyText(
                ChatColor.translateAlternateColorCodes('&', legacy))) {
            if (command != null) {
                part.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            }
            root.addExtra(part);
        }
    }
}

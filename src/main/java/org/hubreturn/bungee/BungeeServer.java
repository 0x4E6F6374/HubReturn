package org.hubreturn.bungee;

import net.md_5.bungee.api.config.ServerInfo;
import org.hubreturn.core.ServerRef;

final class BungeeServer implements ServerRef {

    private final ServerInfo info;

    BungeeServer(ServerInfo info) {
        this.info = info;
    }

    ServerInfo raw() {
        return info;
    }

    @Override
    public String name() {
        return info.getName();
    }

    @Override
    public int players() {
        return info.getPlayers().size();
    }
}

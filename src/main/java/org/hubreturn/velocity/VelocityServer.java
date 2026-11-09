package org.hubreturn.velocity;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.hubreturn.core.ServerRef;

final class VelocityServer implements ServerRef {

    private final RegisteredServer server;

    VelocityServer(RegisteredServer server) {
        this.server = server;
    }

    RegisteredServer raw() {
        return server;
    }

    @Override
    public String name() {
        return server.getServerInfo().getName();
    }

    @Override
    public int players() {
        return server.getPlayersConnected().size();
    }
}

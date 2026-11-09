package org.hubreturn.core;

import java.util.UUID;

public interface PlayerRef extends CommandActor {

    UUID uuid();

    String currentServer();

    void connect(ServerRef target, ConnectCallback callback);

    @FunctionalInterface
    interface ConnectCallback {
        void done(boolean success, String detail);
    }
}

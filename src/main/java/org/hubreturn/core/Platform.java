package org.hubreturn.core;

import java.nio.file.Path;
import java.util.Optional;

public interface Platform {

    Path dataDirectory();

    String platformName();

    Optional<PlayerRef> player(String name);

    Optional<ServerRef> server(String name);

    CommandActor console();

    void registerChannel(String channel);

    void unregisterChannel(String channel);

    void sendToServer(String serverName, String channel, byte[] data);

    void schedule(Runnable task, long delayMillis);

    void info(String text);

    void warn(String text);

    void error(String text, Throwable error);
}

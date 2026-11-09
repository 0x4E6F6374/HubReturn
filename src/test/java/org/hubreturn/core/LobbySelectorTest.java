package org.hubreturn.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LobbySelectorTest {

    @TempDir
    Path dataDir;

    @Test
    void excludesServersAlreadyTried() throws IOException {
        Settings settings = Settings.load(dataDir);
        Platform platform = mock(Platform.class);
        ServerRef lobby = mock(ServerRef.class);
        when(platform.server("lobby")).thenReturn(Optional.of(lobby));

        LobbySelector selector = new LobbySelector(settings, platform);

        assertTrue(selector.pick("survival").isPresent());
        assertTrue(selector.pick("survival", Set.of("lobby")).isEmpty());
    }

    @Test
    void roundRobinCyclesBetweenLobbies() throws IOException {
        Files.writeString(dataDir.resolve("config.yml"),
                "lobbies:\n"
                        + "  strategy: round-robin\n"
                        + "  servers:\n"
                        + "    - name: lobby-a\n"
                        + "      weight: 1\n"
                        + "    - name: lobby-b\n"
                        + "      weight: 1\n");
        Settings settings = Settings.load(dataDir);
        Platform platform = mock(Platform.class);
        ServerRef first = mock(ServerRef.class);
        ServerRef second = mock(ServerRef.class);
        when(platform.server("lobby-a")).thenReturn(Optional.of(first));
        when(platform.server("lobby-b")).thenReturn(Optional.of(second));

        LobbySelector selector = new LobbySelector(settings, platform);

        assertSame(first, selector.pick(null).orElseThrow());
        assertSame(second, selector.pick(null).orElseThrow());
        assertSame(first, selector.pick(null).orElseThrow());
    }
}

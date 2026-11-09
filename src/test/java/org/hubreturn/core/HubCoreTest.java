package org.hubreturn.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HubCoreTest {

    @TempDir
    Path dataDir;

    private Platform platform;
    private PlayerRef player;

    private HubCore start() throws IOException {
        platform = mock(Platform.class);
        when(platform.dataDirectory()).thenReturn(dataDir);
        when(platform.platformName()).thenReturn("test");
        when(platform.player(anyString())).thenReturn(Optional.empty());
        when(platform.server(anyString())).thenReturn(Optional.empty());
        return HubCore.start(platform);
    }

    private void withPlayer() {
        UUID id = UUID.randomUUID();
        player = mock(PlayerRef.class);
        when(player.uuid()).thenReturn(id);
        when(player.name()).thenReturn("Notch");
        when(player.isPlayer()).thenReturn(true);
        when(player.currentServer()).thenReturn("survival");
        when(player.hasPermission(anyString())).thenReturn(true);
        when(platform.player("Notch")).thenReturn(Optional.of(player));
    }

    private void withLobby() {
        ServerRef lobby = mock(ServerRef.class);
        when(lobby.name()).thenReturn("lobby");
        when(platform.server("lobby")).thenReturn(Optional.of(lobby));
    }

    @Test
    void reloadReregistersCommands() throws IOException {
        HubCore core = start();

        verify(platform).registerCommands(core);
        core.reload();
        verify(platform, times(2)).registerCommands(core);
    }

    @Test
    void reloadCancelsPendingConfirmation() throws IOException {
        HubCore core = start();
        withPlayer();
        core.onCommand(player, new String[0]);

        core.reload();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(player, atLeastOnce()).sendMessage(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(text -> text.contains("Configuration reloaded")));
    }

    @Test
    void refusesSecondTransferForSamePlayer() throws IOException {
        Files.writeString(dataDir.resolve("config.yml"),
                "confirm:\n"
                        + "  mode: none\n"
                        + "command:\n"
                        + "  cooldown-millis: 0\n"
                        + "  bypass-cooldown-permission: \"\"\n");
        HubCore core = start();
        withPlayer();
        withLobby();

        core.onCommand(player, new String[0]);
        core.onCommand(player, new String[0]);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(player, atLeastOnce()).sendMessage(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(text -> text.contains("Another return request is in progress")));
    }

    @Test
    void confirmationRechecksPermission() throws IOException {
        HubCore core = start();
        withPlayer();
        withLobby();
        core.onCommand(player, new String[0]);

        when(player.hasPermission("hubreturn.use")).thenReturn(false);
        core.onCommand(player, new String[0]);

        verify(player, never()).connect(any(ServerRef.class), any(PlayerRef.ConnectCallback.class));
    }

    @Test
    void chinesePlayerSeesChinesePrompt() throws IOException {
        HubCore core = start();
        withPlayer();
        when(player.locale()).thenReturn(Locale.CHINESE);

        core.onCommand(player, new String[0]);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(player, atLeastOnce()).sendMessage(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(text -> text.contains("确认")));
    }

    @Test
    void defaultMessagesAreEnglish() throws IOException {
        HubCore core = start();

        assertTrue(core.messages().text("command.reloaded").contains("Configuration reloaded"));
    }
}

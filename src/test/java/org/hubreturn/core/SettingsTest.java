package org.hubreturn.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    @TempDir
    Path dataDir;

    @Test
    void acceptsTemplateConfig() throws IOException {
        Settings settings = Settings.load(dataDir);

        assertDoesNotThrow(settings::validate);
    }

    @Test
    void rejectsUnknownConfirmMode() throws IOException {
        Files.writeString(dataDir.resolve("config.yml"), "confirm:\n  mode: bogus\n");

        Settings settings = Settings.load(dataDir);

        assertThrows(YamlConfig.ConfigProblem.class, settings::validate);
    }

    @Test
    void rejectsNegativeTimeout() throws IOException {
        Files.writeString(dataDir.resolve("config.yml"), "confirm:\n  timeout-millis: -1\n");

        Settings settings = Settings.load(dataDir);

        assertThrows(YamlConfig.ConfigProblem.class, settings::validate);
    }

    @Test
    void rejectsDefaultLanguageWithoutFile() throws IOException {
        Files.writeString(dataDir.resolve("config.yml"), "language:\n  default: fr\n");

        Settings settings = Settings.load(dataDir);

        assertThrows(YamlConfig.ConfigProblem.class, settings::validate);
    }

    @Test
    void joinKeepsInitialServerUnlessItIsTheProxyDefault() throws IOException {
        Settings settings = Settings.load(dataDir);

        assertFalse(settings.joinOverrideExisting());
        assertTrue(settings.joinReplacesInitial(null, List.of("lobby")));
        assertTrue(settings.joinReplacesInitial("LOBBY", List.of("lobby")));
        assertFalse(settings.joinReplacesInitial("survival", List.of("lobby")));
    }

    @Test
    void joinOverrideExistingReplacesAnyInitialServer() throws IOException {
        Files.writeString(dataDir.resolve("config.yml"), "join:\n  override-existing: true\n");

        Settings settings = Settings.load(dataDir);

        assertTrue(settings.joinReplacesInitial("survival", List.of("lobby")));
    }

    @Test
    void kickTakeoverIsOffByDefault() throws IOException {
        Settings settings = Settings.load(dataDir);

        assertFalse(settings.kickEnabled());
    }
}

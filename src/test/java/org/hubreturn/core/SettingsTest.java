package org.hubreturn.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}

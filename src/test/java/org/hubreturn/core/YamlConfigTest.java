package org.hubreturn.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlConfigTest {

    @TempDir
    Path dataDir;

    @Test
    void mergesTemplateDefaults() throws IOException {
        Files.writeString(dataDir.resolve("config.yml"), "debug: true\n");

        YamlConfig config = YamlConfig.load(dataDir, "config.yml");

        assertTrue(config.bool("debug"));
        assertEquals("hub", config.string("command.name"));
    }

    @Test
    void rejectsMissingKey() throws IOException {
        YamlConfig config = YamlConfig.load(dataDir, "config.yml");

        assertThrows(YamlConfig.ConfigProblem.class, () -> config.req("command.nope"));
    }

    @Test
    void rejectsNumericBoolean() throws IOException {
        Files.writeString(dataDir.resolve("config.yml"), "debug: 1\n");

        YamlConfig config = YamlConfig.load(dataDir, "config.yml");

        assertThrows(YamlConfig.ConfigProblem.class, () -> config.bool("debug"));
    }

    @Test
    void acceptsBooleanText() throws IOException {
        Files.writeString(dataDir.resolve("config.yml"), "debug: TRUE\n");

        YamlConfig config = YamlConfig.load(dataDir, "config.yml");

        assertTrue(config.bool("debug"));
    }
}

package org.hubreturn.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownsTest {

    @TempDir
    Path dataDir;

    @Test
    void rollsBackOneKind() throws IOException {
        Cooldowns cooldowns = new Cooldowns();
        cooldowns.configure(Settings.load(dataDir));
        UUID id = UUID.randomUUID();

        cooldowns.mark(id, Cooldowns.Kind.COMMAND);
        assertTrue(cooldowns.remaining(id, Cooldowns.Kind.COMMAND) > 0);

        cooldowns.clear(id, Cooldowns.Kind.COMMAND);
        assertEquals(0, cooldowns.remaining(id, Cooldowns.Kind.COMMAND));
    }

    @Test
    void keepsKindsSeparate() throws IOException {
        Cooldowns cooldowns = new Cooldowns();
        cooldowns.configure(Settings.load(dataDir));
        UUID id = UUID.randomUUID();

        cooldowns.mark(id, Cooldowns.Kind.COMMAND);
        cooldowns.clear(id, Cooldowns.Kind.MESSAGE);

        assertTrue(cooldowns.remaining(id, Cooldowns.Kind.COMMAND) > 0);
    }
}

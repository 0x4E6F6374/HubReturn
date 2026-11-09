package org.hubreturn.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationsTest {

    @Test
    void refusesSecondPending() {
        Confirmations confirmations = new Confirmations();
        UUID id = UUID.randomUUID();
        Confirmations.Pending pending = pending(id);

        assertTrue(confirmations.put(id, pending));
        assertFalse(confirmations.put(id, pending));
    }

    @Test
    void drainsEverything() {
        Confirmations confirmations = new Confirmations();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        confirmations.put(first, pending(first));
        confirmations.put(second, pending(second));

        assertEquals(2, confirmations.drain().size());
        assertFalse(confirmations.has(first));
        assertFalse(confirmations.has(second));
    }

    @Test
    void takesOnlyExpiredPending() {
        Confirmations confirmations = new Confirmations();
        UUID id = UUID.randomUUID();
        long now = System.currentTimeMillis();
        confirmations.put(id, new Confirmations.Pending(
                id, "Notch", Confirmations.Source.MESSAGE, "survival", now - 1));

        assertTrue(confirmations.takeExpired(id, now).isPresent());
        assertFalse(confirmations.has(id));
    }

    private static Confirmations.Pending pending(UUID id) {
        return new Confirmations.Pending(id, "Notch", Confirmations.Source.COMMAND, "",
                System.currentTimeMillis() + 10_000L);
    }
}

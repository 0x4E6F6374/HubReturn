package org.hubreturn.core;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryWatchTest {

    @Test
    void refusesSecondWatchForSamePlayer() {
        DeliveryWatch delivery = new DeliveryWatch();
        UUID id = UUID.randomUUID();

        assertNotNull(delivery.await(id, "lobby", arrived -> {}));
        assertNull(delivery.await(id, "lobby", arrived -> {}));
    }

    @Test
    void completesOnlyOnce() {
        DeliveryWatch delivery = new DeliveryWatch();
        UUID id = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();
        DeliveryWatch.Watch watch = delivery.await(id, "lobby", arrived -> calls.incrementAndGet());

        delivery.arrived(id, "lobby");
        assertFalse(delivery.expire(id, watch));

        assertEquals(1, calls.get());
    }

    @Test
    void expireReportsMissingArrival() {
        DeliveryWatch delivery = new DeliveryWatch();
        UUID id = UUID.randomUUID();
        AtomicBoolean arrived = new AtomicBoolean(true);
        DeliveryWatch.Watch watch = delivery.await(id, "lobby", value -> arrived.set(value));

        assertTrue(delivery.expire(id, watch));
        assertFalse(arrived.get());
    }

    @Test
    void clearDropsWaitingWithoutCallback() {
        DeliveryWatch delivery = new DeliveryWatch();
        UUID id = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();
        DeliveryWatch.Watch watch = delivery.await(id, "lobby", arrived -> calls.incrementAndGet());

        delivery.clear();

        assertFalse(delivery.expire(id, watch));
        assertEquals(0, calls.get());
    }
}

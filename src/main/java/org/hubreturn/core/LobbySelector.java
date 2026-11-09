package org.hubreturn.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public final class LobbySelector {

    private final Settings settings;
    private final Platform platform;
    private final AtomicInteger cursor = new AtomicInteger();

    public LobbySelector(Settings settings, Platform platform) {
        checkStrategy(settings.lobbyStrategy());
        this.settings = settings;
        this.platform = platform;
    }

    public Optional<ServerRef> pick(String current) {
        List<Candidate> pool = new ArrayList<>();
        for (Settings.LobbyEntry entry : settings.lobbyEntries()) {
            if (settings.skipCurrent() && current != null && entry.name().equalsIgnoreCase(current)) {
                continue;
            }
            platform.server(entry.name()).ifPresent(server -> pool.add(new Candidate(server, entry.weight())));
        }
        if (pool.isEmpty()) {
            return Optional.empty();
        }

        switch (settings.lobbyStrategy()) {
            case "least-players":
                pool.sort(Comparator.comparingInt(item -> item.server.players()));
                return Optional.of(pool.get(0).server);
            case "round-robin":
                return Optional.of(pool.get(Math.floorMod(cursor.getAndIncrement(), pool.size())).server);
            case "random":
                return Optional.of(pool.get(ThreadLocalRandom.current().nextInt(pool.size())).server);
            case "weighted":
                return Optional.of(weighted(pool).server);
            case "first":
                return Optional.of(pool.get(0).server);
            default:
                throw new IllegalStateException("未知的选服策略: " + settings.lobbyStrategy());
        }
    }

    private static Candidate weighted(List<Candidate> pool) {
        int total = 0;
        for (Candidate item : pool) {
            total += item.weight;
        }
        int roll = ThreadLocalRandom.current().nextInt(Math.max(total, 1));
        for (Candidate item : pool) {
            roll -= item.weight;
            if (roll < 0) {
                return item;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private static void checkStrategy(String value) {
        switch (value) {
            case "least-players":
            case "round-robin":
            case "random":
            case "weighted":
            case "first":
                return;
            default:
                throw new IllegalArgumentException("未知的选服策略: " + value);
        }
    }

    private static final class Candidate {
        private final ServerRef server;
        private final int weight;

        Candidate(ServerRef server, int weight) {
            this.server = server;
            this.weight = weight;
        }
    }
}

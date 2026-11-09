package org.hubreturn.core;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Confirmations {

    public enum Source {
        COMMAND,
        MESSAGE
    }

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public boolean has(UUID id) {
        return pending.containsKey(id);
    }

    public boolean put(UUID id, Pending value) {
        return pending.putIfAbsent(id, value) == null;
    }

    public Optional<Pending> take(UUID id) {
        return Optional.ofNullable(pending.remove(id));
    }

    public Optional<Pending> takeExpired(UUID id, long now) {
        Pending value = pending.get(id);
        if (value == null || value.deadline() > now) {
            return Optional.empty();
        }
        return pending.remove(id, value) ? Optional.of(value) : Optional.empty();
    }

    public long remaining(UUID id, long now) {
        Pending value = pending.get(id);
        return value == null ? 0 : Math.max(value.deadline() - now, 0);
    }

    public Map<UUID, Pending> drain() {
        Map<UUID, Pending> out = new ConcurrentHashMap<>(pending);
        pending.clear();
        return out;
    }

    public record Pending(UUID id, String playerName, Source source, String replySource,
                          long deadline, Locale locale) {
    }
}

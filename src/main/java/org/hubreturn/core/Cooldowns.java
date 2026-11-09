package org.hubreturn.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Cooldowns {

    public enum Kind {
        COMMAND,
        MESSAGE
    }

    private final Map<Key, Long> last = new ConcurrentHashMap<>();
    private volatile long commandMillis;
    private volatile long messageMillis;

    public void configure(Settings settings) {
        commandMillis = settings.commandCooldownMillis();
        messageMillis = settings.messageCooldownMillis();
    }

    public long remaining(UUID id, Kind kind) {
        long window = kind == Kind.COMMAND ? commandMillis : messageMillis;
        if (window <= 0) {
            return 0;
        }
        Long prev = last.get(new Key(id, kind));
        if (prev == null) {
            return 0;
        }
        return Math.max(window - (System.currentTimeMillis() - prev), 0);
    }

    public void mark(UUID id, Kind kind) {
        last.put(new Key(id, kind), System.currentTimeMillis());
    }

    public void clear() {
        last.clear();
    }

    private record Key(UUID id, Kind kind) {
    }
}

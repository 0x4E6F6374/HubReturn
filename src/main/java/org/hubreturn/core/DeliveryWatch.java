package org.hubreturn.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class DeliveryWatch {

    private final Map<UUID, Watch> waiting = new ConcurrentHashMap<>();

    public Watch await(UUID id, String server, Consumer<Boolean> done) {
        Watch watch = new Watch(server, done);
        waiting.put(id, watch);
        return watch;
    }

    public void arrived(UUID id, String server) {
        Watch watch = waiting.get(id);
        if (watch != null && watch.server.equalsIgnoreCase(server) && waiting.remove(id, watch)) {
            watch.complete(true);
        }
    }

    public boolean expire(UUID id, Watch expected) {
        Watch watch = waiting.get(id);
        if (watch != expected || !waiting.remove(id, watch)) {
            return false;
        }
        watch.complete(false);
        return true;
    }

    public void cancel(UUID id) {
        waiting.remove(id);
    }

    public void clear() {
        waiting.clear();
    }

    static final class Watch {
        private final String server;
        private final Consumer<Boolean> done;
        private final AtomicBoolean finished = new AtomicBoolean();

        Watch(String server, Consumer<Boolean> done) {
            this.server = server;
            this.done = done;
        }

        void complete(boolean arrived) {
            if (finished.compareAndSet(false, true)) {
                done.accept(arrived);
            }
        }
    }
}

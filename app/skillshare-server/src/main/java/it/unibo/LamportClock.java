package it.unibo;

import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.Serializer;

/**
 * A Lamport logical clock for this instance, persisted in MapDB so it survives
 * restarts. It assigns a happens-before-consistent ordering to federated events
 * (currently chat messages) without relying on synchronized wall clocks.
 *
 * Rules:
 *  - tick():            a local event occurs  -> value = value + 1
 *  - update(received):  a message arrives     -> value = max(value, received) + 1
 *
 * A total order across instances is obtained by comparing the tuple
 * (lamportTimestamp, instanceId), so even concurrent events (equal timestamps)
 * get a deterministic order.
 *
 * The counter lives in MapDB and is shared by every LamportClock instance in
 * this JVM (one JVM == one federated instance), coordinated by a static lock.
 */
public class LamportClock {

    private static final String KEY = "value";
    private static final Object LOCK = new Object();

    private final ConcurrentMap<String, Long> store;

    @SuppressWarnings("unchecked")
    public LamportClock() {
        DB db = DatabaseCore.getDB();
        store = (ConcurrentMap<String, Long>) (ConcurrentMap<?, ?>) db
                .hashMap("lamportClock", Serializer.STRING, Serializer.LONG)
                .createOrOpen();
    }

    /** A local event: advance the clock and return the new value. */
    public long tick() {
        synchronized (LOCK) {
            long next = current() + 1;
            persist(next);
            return next;
        }
    }

    /** Incorporate a received timestamp: value = max(value, received) + 1. */
    public long update(long received) {
        synchronized (LOCK) {
            long next = Math.max(current(), received) + 1;
            persist(next);
            return next;
        }
    }

    /** Current value without advancing the clock. */
    public long current() {
        Long value = store.get(KEY);
        return value == null ? 0L : value;
    }

    private void persist(long value) {
        store.put(KEY, value);
        DatabaseCore.commit();
    }
}
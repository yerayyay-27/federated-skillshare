package it.unibo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.Serializer;

/** Persistent ledger of federation events already applied by this instance. */
public class FederationInboxRepository {

    private final ConcurrentMap<String, ProcessedFederationEvent> processedEvents;

    @SuppressWarnings("unchecked")
    public FederationInboxRepository() {
        DB db = DatabaseCore.getDB();
        processedEvents = (ConcurrentMap<String, ProcessedFederationEvent>) (ConcurrentMap<?, ?>) db
                .hashMap("processedFederationEvents", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();
    }

    public boolean markProcessed(FederationEvent event) {
        validateEvent(event);
        ProcessedFederationEvent processed = new ProcessedFederationEvent(
                event.getEventId(),
                event.getType(),
                event.getOriginInstance(),
                Instant.now());
        ProcessedFederationEvent existing = processedEvents.putIfAbsent(
                event.getEventId(), processed);
        if (existing != null) {
            return false;
        }
        DatabaseCore.commit();
        return true;
    }

    public boolean hasProcessed(String eventId) {
        return eventId != null
                && !eventId.trim().isEmpty()
                && processedEvents.containsKey(eventId);
    }

    public ProcessedFederationEvent findById(String eventId) {
        return processedEvents.get(eventId);
    }

    public List<ProcessedFederationEvent> findAll() {
        return new ArrayList<>(processedEvents.values());
    }

    public boolean delete(String eventId) {
        ProcessedFederationEvent removed = processedEvents.remove(eventId);
        if (removed == null) {
            return false;
        }
        DatabaseCore.commit();
        return true;
    }

    private void validateEvent(FederationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Federation event must not be null");
        }
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            throw new IllegalArgumentException("Federation event id must not be blank");
        }
    }
}

package it.unibo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.mapdb.DB;
import org.mapdb.Serializer;

public class FederationOutboxRepository {

    private final ConcurrentMap<String, OutgoingFederationEvent> outbox;

    @SuppressWarnings("unchecked")
    public FederationOutboxRepository() {
        DB db = DatabaseCore.getDB();
        outbox = (ConcurrentMap<String, OutgoingFederationEvent>) (ConcurrentMap<?, ?>) db
                .hashMap("federationOutbox", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();
    }

    public boolean save(OutgoingFederationEvent event) {
        validate(event);
        OutgoingFederationEvent existing = outbox.putIfAbsent(event.getId(), event);
        if (existing != null) {
            return false;
        }
        DatabaseCore.commit();
        return true;
    }

    public void upsert(OutgoingFederationEvent event) {
        validate(event);
        outbox.put(event.getId(), event);
        DatabaseCore.commit();
    }

    public OutgoingFederationEvent findById(String id) {
        return outbox.get(id);
    }

    public List<OutgoingFederationEvent> findAll() {
        return new ArrayList<>(outbox.values());
    }

    public List<OutgoingFederationEvent> findPending() {
        return findByStatus(FederationDeliveryStatus.PENDING);
    }

    public List<OutgoingFederationEvent> findFailed() {
        return findByStatus(FederationDeliveryStatus.FAILED);
    }

    public void update(OutgoingFederationEvent event) {
        validate(event);
        outbox.put(event.getId(), event);
        DatabaseCore.commit();
    }

    public boolean delete(String id) {
        OutgoingFederationEvent removed = outbox.remove(id);
        if (removed == null) {
            return false;
        }
        DatabaseCore.commit();
        return true;
    }

    private List<OutgoingFederationEvent> findByStatus(FederationDeliveryStatus status) {
        return outbox.values().stream()
                .filter(event -> status.equals(event.getStatus()))
                .collect(Collectors.toList());
    }

    private void validate(OutgoingFederationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Outgoing federation event must not be null");
        }
    }
}

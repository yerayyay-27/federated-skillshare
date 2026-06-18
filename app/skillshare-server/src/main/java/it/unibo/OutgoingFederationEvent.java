package it.unibo;

import java.io.Serializable;
import java.time.Instant;

public class OutgoingFederationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private FederationEvent event;
    private String targetPeerUrl;
    private FederationDeliveryStatus status;
    private int attemptCount;
    private Instant createdAt;
    private Instant lastAttemptAt;
    private Instant nextAttemptAt;
    private String lastError;

    public OutgoingFederationEvent() {
        // Required by serializers.
    }

    public OutgoingFederationEvent(
            String id,
            FederationEvent event,
            String targetPeerUrl,
            FederationDeliveryStatus status,
            int attemptCount,
            Instant createdAt,
            Instant lastAttemptAt,
            Instant nextAttemptAt,
            String lastError) {
        this.id = requireNotBlank(id, "Outgoing event id must not be blank");
        this.event = requireNotNull(event, "Federation event must not be null");
        this.targetPeerUrl = requireNotBlank(targetPeerUrl, "Target peer URL must not be blank");
        this.status = requireNotNull(status, "Delivery status must not be null");
        if (attemptCount < 0) {
            throw new IllegalArgumentException("Attempt count must not be negative");
        }
        this.attemptCount = attemptCount;
        this.createdAt = requireNotNull(createdAt, "Created timestamp must not be null");
        this.lastAttemptAt = lastAttemptAt;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
    }

    public static OutgoingFederationEvent pending(
            String id,
            FederationEvent event,
            String targetPeerUrl,
            Instant createdAt) {
        return new OutgoingFederationEvent(
                id,
                event,
                targetPeerUrl,
                FederationDeliveryStatus.PENDING,
                0,
                createdAt,
                null,
                null,
                null);
    }

    public void markDelivered(Instant at) {
        Instant timestamp = requireNotNull(at, "Delivery timestamp must not be null");
        this.status = FederationDeliveryStatus.DELIVERED;
        this.attemptCount++;
        this.lastAttemptAt = timestamp;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public void markFailed(String error, Instant attemptedAt, Instant nextAttemptAt) {
        this.status = FederationDeliveryStatus.FAILED;
        this.attemptCount++;
        this.lastAttemptAt = requireNotNull(attemptedAt, "Attempt timestamp must not be null");
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = error == null ? "" : error;
    }

    public void markPending() {
        this.status = FederationDeliveryStatus.PENDING;
        this.nextAttemptAt = null;
        this.lastError = null;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public FederationEvent getEvent() { return event; }
    public void setEvent(FederationEvent event) { this.event = event; }

    public String getTargetPeerUrl() { return targetPeerUrl; }
    public void setTargetPeerUrl(String targetPeerUrl) { this.targetPeerUrl = targetPeerUrl; }

    public FederationDeliveryStatus getStatus() { return status; }
    public void setStatus(FederationDeliveryStatus status) { this.status = status; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }

    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    private static String requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static <T> T requireNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}

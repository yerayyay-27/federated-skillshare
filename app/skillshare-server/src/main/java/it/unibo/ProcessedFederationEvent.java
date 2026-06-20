package it.unibo;

import java.io.Serializable;
import java.time.Instant;

public class ProcessedFederationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;
    private String eventType;
    private String originInstance;
    private Instant processedAt;

    public ProcessedFederationEvent() {
        // Required by serializers.
    }

    public ProcessedFederationEvent(
            String eventId,
            String eventType,
            String originInstance,
            Instant processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.originInstance = originInstance;
        this.processedAt = processedAt;
    }

    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getOriginInstance() { return originInstance; }
    public Instant getProcessedAt() { return processedAt; }
}

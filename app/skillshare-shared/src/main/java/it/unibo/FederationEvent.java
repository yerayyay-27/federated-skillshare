package it.unibo;

import java.io.Serializable;

/**
 * An event exchanged between federated instances.
 * Instead of replicating raw database rows, instances notify each other of
 * domain events. The payload depends on the event type; "originInstance"
 * records which instance produced the event.
 *
 * Two event families share this envelope:
 *  - Announcement events: replicated to every peer (the marketplace is global).
 *  - Exchange events: conceptually directed, but still sent through the same
 *    broadcast/outbox path. Each inbox decides whether the event concerns it by
 *    comparing its own instance id against the request's fromInstance /
 *    toInstance, so no extra "target" field is needed.
 */
public class FederationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_ANNOUNCEMENT_CREATED = "AnnouncementCreated";
    public static final String TYPE_ANNOUNCEMENT_UPDATED = "AnnouncementUpdated";
    public static final String TYPE_ANNOUNCEMENT_DELETED = "AnnouncementDeleted";

    public static final String TYPE_EXCHANGE_REQUESTED = "ExchangeRequested";
    public static final String TYPE_EXCHANGE_ACCEPTED = "ExchangeAccepted";
    public static final String TYPE_EXCHANGE_REJECTED = "ExchangeRejected";

    private String type;
    private String originInstance;
    private Announcement announcement;          // payload for Announcement Created / Updated
    private String announcementId;              // payload for Announcement Deleted
    private ExchangeRequest exchangeRequest;    // payload for all Exchange events

    public FederationEvent() {
    }

    public static FederationEvent announcementCreated(String originInstance, Announcement announcement) {
        FederationEvent event = new FederationEvent();
        event.type = TYPE_ANNOUNCEMENT_CREATED;
        event.originInstance = originInstance;
        event.announcement = announcement;
        return event;
    }

    public static FederationEvent announcementUpdated(String originInstance, Announcement announcement) {
        FederationEvent event = new FederationEvent();
        event.type = TYPE_ANNOUNCEMENT_UPDATED;
        event.originInstance = originInstance;
        event.announcement = announcement;
        return event;
    }

    public static FederationEvent announcementDeleted(String originInstance, String announcementId) {
        FederationEvent event = new FederationEvent();
        event.type = TYPE_ANNOUNCEMENT_DELETED;
        event.originInstance = originInstance;
        event.announcementId = announcementId;
        return event;
    }

    /**
     * A user on the requester's instance asks for an exchange. Sent so the
     * announcement owner's instance (the authoritative replica) stores it.
     * The whole ExchangeRequest is carried, so delivery is idempotent.
     */
    public static FederationEvent exchangeRequested(String originInstance, ExchangeRequest exchangeRequest) {
        FederationEvent event = new FederationEvent();
        event.type = TYPE_EXCHANGE_REQUESTED;
        event.originInstance = originInstance;
        event.exchangeRequest = exchangeRequest;
        return event;
    }

    /**
     * The owner accepted the exchange. The updated request (status = ACCEPTED)
     * is carried whole, so the requester's instance overwrites its local
     * replica's status and the two converge.
     */
    public static FederationEvent exchangeAccepted(String originInstance, ExchangeRequest exchangeRequest) {
        FederationEvent event = new FederationEvent();
        event.type = TYPE_EXCHANGE_ACCEPTED;
        event.originInstance = originInstance;
        event.exchangeRequest = exchangeRequest;
        return event;
    }

    /** The owner rejected the exchange. Same convergence logic as accepted. */
    public static FederationEvent exchangeRejected(String originInstance, ExchangeRequest exchangeRequest) {
        FederationEvent event = new FederationEvent();
        event.type = TYPE_EXCHANGE_REJECTED;
        event.originInstance = originInstance;
        event.exchangeRequest = exchangeRequest;
        return event;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOriginInstance() { return originInstance; }
    public void setOriginInstance(String originInstance) { this.originInstance = originInstance; }

    public Announcement getAnnouncement() { return announcement; }
    public void setAnnouncement(Announcement announcement) { this.announcement = announcement; }

    public String getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(String announcementId) { this.announcementId = announcementId; }

    public ExchangeRequest getExchangeRequest() { return exchangeRequest; }
    public void setExchangeRequest(ExchangeRequest exchangeRequest) { this.exchangeRequest = exchangeRequest; }
}
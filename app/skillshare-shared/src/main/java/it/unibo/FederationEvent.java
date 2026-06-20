package it.unibo;

import java.io.Serializable;
import java.util.UUID;

/**
 * An event exchanged between federated instances.
 * Instead of replicating raw database rows, instances notify each other of
 * domain events. The payload depends on the event type; "originInstance"
 * records which instance produced the event.
 *
 * Event families sharing this envelope:
 *  - Announcement events: replicated to every peer (the marketplace is global).
 *  - Exchange events: conceptually directed, but still sent through the same
 *    broadcast/outbox path. Each inbox decides whether the event concerns it by
 *    comparing its own instance id against the request's fromInstance /
 *    toInstance, so no extra "target" field is needed.
 *  - Chat events: a single message in an accepted exchange's conversation,
 *    sent to the other participant's instance so both replicas converge.
 *
 * Every event carries a unique "eventId" so the receiving inbox can apply it
 * at most once (idempotency under at-least-once delivery).
 */
public class FederationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_ANNOUNCEMENT_CREATED = "AnnouncementCreated";
    public static final String TYPE_ANNOUNCEMENT_UPDATED = "AnnouncementUpdated";
    public static final String TYPE_ANNOUNCEMENT_DELETED = "AnnouncementDeleted";

    public static final String TYPE_EXCHANGE_REQUESTED = "ExchangeRequested";
    public static final String TYPE_EXCHANGE_ACCEPTED = "ExchangeAccepted";
    public static final String TYPE_EXCHANGE_REJECTED = "ExchangeRejected";

    public static final String TYPE_CHAT_MESSAGE_CREATED = "ChatMessageCreated";
    public static final String TYPE_REVIEW_CREATED = "ReviewCreated";

    private String eventId;
    private String type;
    private String originInstance;
    private Announcement announcement;          // payload for Announcement Created / Updated
    private String announcementId;              // payload for Announcement Deleted
    private ExchangeRequest exchangeRequest;    // payload for all Exchange events
    private ChatMessage chatMessage;            // payload for Chat events
    private Review review;                      // payload for Review events

    public FederationEvent() {
    }

    public static FederationEvent announcementCreated(String originInstance, Announcement announcement) {
        FederationEvent event = newEvent(TYPE_ANNOUNCEMENT_CREATED, originInstance);
        event.announcement = announcement;
        return event;
    }

    public static FederationEvent announcementUpdated(String originInstance, Announcement announcement) {
        FederationEvent event = newEvent(TYPE_ANNOUNCEMENT_UPDATED, originInstance);
        event.announcement = announcement;
        return event;
    }

    public static FederationEvent announcementDeleted(String originInstance, String announcementId) {
        FederationEvent event = newEvent(TYPE_ANNOUNCEMENT_DELETED, originInstance);
        event.announcementId = announcementId;
        return event;
    }

    /**
     * A user on the requester's instance asks for an exchange. Sent so the
     * announcement owner's instance (the authoritative replica) stores it.
     * The whole ExchangeRequest is carried, so delivery is idempotent.
     */
    public static FederationEvent exchangeRequested(String originInstance, ExchangeRequest exchangeRequest) {
        FederationEvent event = newEvent(TYPE_EXCHANGE_REQUESTED, originInstance);
        event.exchangeRequest = exchangeRequest;
        return event;
    }

    /**
     * The owner accepted the exchange. The updated request (status = ACCEPTED)
     * is carried whole, so the requester's instance overwrites its local
     * replica's status and the two converge.
     */
    public static FederationEvent exchangeAccepted(String originInstance, ExchangeRequest exchangeRequest) {
        FederationEvent event = newEvent(TYPE_EXCHANGE_ACCEPTED, originInstance);
        event.exchangeRequest = exchangeRequest;
        return event;
    }

    /** The owner rejected the exchange. Same convergence logic as accepted. */
    public static FederationEvent exchangeRejected(String originInstance, ExchangeRequest exchangeRequest) {
        FederationEvent event = newEvent(TYPE_EXCHANGE_REJECTED, originInstance);
        event.exchangeRequest = exchangeRequest;
        return event;
    }

    /**
     * A chat message in an accepted exchange. Sent to the other participant's
     * instance, which appends it to its replica of the conversation. The
     * eventId makes repeated delivery safe (chat storage only appends).
     */
    public static FederationEvent chatMessageCreated(String originInstance, ChatMessage chatMessage) {
        FederationEvent event = newEvent(TYPE_CHAT_MESSAGE_CREATED, originInstance);
        event.chatMessage = chatMessage;
        return event;
    }

    public static FederationEvent reviewCreated(String originInstance, Review review) {
        FederationEvent event = newEvent(TYPE_REVIEW_CREATED, originInstance);
        event.review = review;
        return event;
    }

    void ensureEventId() {
        if (eventId == null || eventId.trim().isEmpty()) {
            eventId = UUID.randomUUID().toString();
        }
    }

    private static FederationEvent newEvent(String type, String originInstance) {
        FederationEvent event = new FederationEvent();
        event.ensureEventId();
        event.type = type;
        event.originInstance = originInstance;
        return event;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

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

    public ChatMessage getChatMessage() { return chatMessage; }
    public void setChatMessage(ChatMessage chatMessage) { this.chatMessage = chatMessage; }

    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
}

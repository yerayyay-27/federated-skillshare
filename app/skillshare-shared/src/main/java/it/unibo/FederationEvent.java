package it.unibo;

import java.io.Serializable;

/**
 * An event exchanged between federated instances.
 * Instead of replicating raw database rows, instances notify each other of
 * domain events (announcement created, updated or deleted). The payload
 * depends on the event type; "originInstance" records which instance
 * produced the event.
 */
public class FederationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_ANNOUNCEMENT_CREATED = "AnnouncementCreated";
    public static final String TYPE_ANNOUNCEMENT_UPDATED = "AnnouncementUpdated";
    public static final String TYPE_ANNOUNCEMENT_DELETED = "AnnouncementDeleted";

    private String type;
    private String originInstance;
    private Announcement announcement;   // payload for Created / Updated
    private String announcementId;       // payload for Deleted

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

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOriginInstance() { return originInstance; }
    public void setOriginInstance(String originInstance) { this.originInstance = originInstance; }

    public Announcement getAnnouncement() { return announcement; }
    public void setAnnouncement(Announcement announcement) { this.announcement = announcement; }

    public String getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(String announcementId) { this.announcementId = announcementId; }
}
package it.unibo;

import java.io.Serializable;

/**
 * An event exchanged between federated instances.
 * Instead of replicating raw database rows, instances notify each other of
 * domain events (e.g. an announcement was created). The payload is the
 * affected entity; "type" tells the receiver how to interpret it, and
 * "originInstance" records which instance produced the event.
 */
public class FederationEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_ANNOUNCEMENT_CREATED = "AnnouncementCreated";

    private String type;
    private String originInstance;
    private Announcement announcement; // payload for AnnouncementCreated

    public FederationEvent() {
    }

    public FederationEvent(String type, String originInstance, Announcement announcement) {
        this.type = type;
        this.originInstance = originInstance;
        this.announcement = announcement;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOriginInstance() { return originInstance; }
    public void setOriginInstance(String originInstance) { this.originInstance = originInstance; }

    public Announcement getAnnouncement() { return announcement; }
    public void setAnnouncement(Announcement announcement) { this.announcement = announcement; }
}
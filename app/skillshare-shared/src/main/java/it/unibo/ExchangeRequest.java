package it.unibo;

import java.io.Serializable;

public class ExchangeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String id;
    private String announcementId;
    private String announcementOfferedSkill; // snapshot for display convenience
    private String fromUsername;             // the requester (student)
    private String toUsername;               // the announcement owner (teacher)
    private String message;
    private String status;

    public ExchangeRequest() {
    }

    public ExchangeRequest(
            String id,
            String announcementId,
            String announcementOfferedSkill,
            String fromUsername,
            String toUsername,
            String message,
            String status) {
        this.id = id;
        this.announcementId = announcementId;
        this.announcementOfferedSkill = announcementOfferedSkill;
        this.fromUsername = fromUsername;
        this.toUsername = toUsername;
        this.message = message;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAnnouncementId() { return announcementId; }
    public void setAnnouncementId(String announcementId) { this.announcementId = announcementId; }

    public String getAnnouncementOfferedSkill() { return announcementOfferedSkill; }
    public void setAnnouncementOfferedSkill(String s) { this.announcementOfferedSkill = s; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
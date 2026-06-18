package it.unibo;

import java.io.Serializable;

public class Announcement implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String ownerUsername;
    private String offeredSkill;
    private String requestedSkill;
    private String description;
    private String availability;
    private boolean active;
    private String originInstance; // home instance of this announcement (federated identity)

    public Announcement() {
    }

    public Announcement(
            String id,
            String ownerUsername,
            String offeredSkill,
            String requestedSkill,
            String description,
            String availability,
            boolean active) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.offeredSkill = offeredSkill;
        this.requestedSkill = requestedSkill;
        this.description = description;
        this.availability = availability;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getOfferedSkill() { return offeredSkill; }
    public void setOfferedSkill(String offeredSkill) { this.offeredSkill = offeredSkill; }

    public String getRequestedSkill() { return requestedSkill; }
    public void setRequestedSkill(String requestedSkill) { this.requestedSkill = requestedSkill; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getOriginInstance() { return originInstance; }
    public void setOriginInstance(String originInstance) { this.originInstance = originInstance; }
}
package it.unibo;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AnnouncementManager {

    private final AnnouncementRepository repository;

    public AnnouncementManager() {
        this(new AnnouncementRepository());
    }

    public AnnouncementManager(AnnouncementRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("Announcement repository must not be null");
        }
        this.repository = repository;
    }

    public Announcement createAnnouncement(Announcement announcement) {
        validateAnnouncement(announcement);
        if (!repository.save(announcement)) {
            throw new IllegalArgumentException("Announcement id already exists");
        }

        return announcement;
    }

    public Announcement getAnnouncementById(String id) {
        return repository.findById(id);
    }

    public List<Announcement> getActiveAnnouncements() {
        return repository.listAll().stream()
                .filter(Announcement::isActive)
                .collect(Collectors.toList());
    }

    public boolean deactivateAnnouncement(String id) {
        return repository.deactivateById(id);
    }

    public List<Announcement> searchActiveAnnouncements(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return getActiveAnnouncements().stream()
                .filter(announcement -> containsIgnoreCase(announcement.getOfferedSkill(), normalizedQuery)
                        || containsIgnoreCase(announcement.getRequestedSkill(), normalizedQuery)
                        || containsIgnoreCase(announcement.getDescription(), normalizedQuery))
                .collect(Collectors.toList());
    }

    private void validateAnnouncement(Announcement announcement) {
        if (announcement == null) {
            throw new IllegalArgumentException("Announcement must not be null");
        }
        requireNotBlank(announcement.getId(), "Announcement id must not be blank");
        requireNotBlank(announcement.getOwnerUsername(), "Owner username must not be blank");
        requireNotBlank(announcement.getOfferedSkill(), "Offered skill must not be blank");
        requireNotBlank(announcement.getRequestedSkill(), "Requested skill must not be blank");
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean containsIgnoreCase(String value, String normalizedQuery) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }
}

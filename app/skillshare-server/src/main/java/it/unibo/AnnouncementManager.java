package it.unibo;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AnnouncementManager {

    private final AnnouncementRepository repository;
    private final Supplier<String> idGenerator;

    public AnnouncementManager() {
        this(new AnnouncementRepository(), () -> UUID.randomUUID().toString());
    }

    public AnnouncementManager(AnnouncementRepository repository) {
        this(repository, () -> UUID.randomUUID().toString());
    }

    AnnouncementManager(
            AnnouncementRepository repository,
            Supplier<String> idGenerator) {
        if (repository == null) {
            throw new IllegalArgumentException("Announcement repository must not be null");
        }
        if (idGenerator == null) {
            throw new IllegalArgumentException("Announcement id generator must not be null");
        }
        this.repository = repository;
        this.idGenerator = idGenerator;
    }

    public Announcement createAnnouncement(Announcement announcement) {
        validateAnnouncementDraft(announcement);
        announcement.setActive(true);

        boolean inserted;
        do {
            String generatedId = idGenerator.get();
            requireNotBlank(generatedId, "Generated announcement id must not be blank");
            announcement.setId(generatedId);
            inserted = repository.save(announcement);
        } while (!inserted);

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

    public Announcement updateAnnouncement(
            String ownerUsername,
            Announcement announcement) {
        requireNotBlank(ownerUsername, "Owner username must not be blank");
        validateAnnouncementForUpdate(announcement);

        Announcement storedAnnouncement = repository.findById(announcement.getId());
        if (storedAnnouncement == null) {
            throw new IllegalArgumentException("Announcement not found");
        }
        requireOwner(storedAnnouncement, ownerUsername);

        Announcement updatedAnnouncement = new Announcement(
                storedAnnouncement.getId(),
                storedAnnouncement.getOwnerUsername(),
                announcement.getOfferedSkill(),
                announcement.getRequestedSkill(),
                announcement.getDescription(),
                announcement.getAvailability(),
                storedAnnouncement.isActive());

        if (!repository.update(updatedAnnouncement)) {
            throw new IllegalArgumentException("Announcement not found");
        }
        return updatedAnnouncement;
    }

    public boolean deleteAnnouncement(String id, String ownerUsername) {
        requireNotBlank(id, "Announcement id must not be blank");
        requireNotBlank(ownerUsername, "Owner username must not be blank");

        Announcement storedAnnouncement = repository.findById(id);
        if (storedAnnouncement == null) {
            throw new IllegalArgumentException("Announcement not found");
        }
        requireOwner(storedAnnouncement, ownerUsername);

        if (!repository.deleteById(id)) {
            throw new IllegalArgumentException("Announcement not found");
        }
        return true;
    }

    private void validateAnnouncementDraft(Announcement announcement) {
        if (announcement == null) {
            throw new IllegalArgumentException("Announcement must not be null");
        }
        requireNotBlank(announcement.getOwnerUsername(), "Owner username must not be blank");
        requireNotBlank(announcement.getOfferedSkill(), "Offered skill must not be blank");
        requireNotBlank(announcement.getRequestedSkill(), "Requested skill must not be blank");
    }

    private void validateAnnouncementForUpdate(Announcement announcement) {
        if (announcement == null) {
            throw new IllegalArgumentException("Announcement must not be null");
        }
        requireNotBlank(announcement.getId(), "Announcement id must not be blank");
        requireNotBlank(announcement.getOfferedSkill(), "Offered skill must not be blank");
        requireNotBlank(announcement.getRequestedSkill(), "Requested skill must not be blank");
    }

    private void requireOwner(Announcement announcement, String ownerUsername) {
        if (!ownerUsername.equals(announcement.getOwnerUsername())) {
            throw new IllegalArgumentException("Only the announcement owner can perform this action");
        }
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

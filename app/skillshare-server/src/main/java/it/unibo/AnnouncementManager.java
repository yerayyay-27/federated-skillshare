package it.unibo;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AnnouncementManager {

    private final AnnouncementRepository repository;
    private final Supplier<String> idGenerator;
    private final FederationClient federationClient;

    public AnnouncementManager() {
        this(new AnnouncementRepository(), () -> UUID.randomUUID().toString());
    }

    public AnnouncementManager(AnnouncementRepository repository) {
        this(repository, () -> UUID.randomUUID().toString());
    }

    AnnouncementManager(
            AnnouncementRepository repository,
            Supplier<String> idGenerator) {
        this(repository, idGenerator, new FederationClient());
    }

    // Full constructor for tests: a federation client can be injected
    // (e.g. a no-op) so unit tests don't perform real network calls.
    AnnouncementManager(
            AnnouncementRepository repository,
            Supplier<String> idGenerator,
            FederationClient federationClient) {
        if (repository == null) {
            throw new IllegalArgumentException("Announcement repository must not be null");
        }
        if (idGenerator == null) {
            throw new IllegalArgumentException("Announcement id generator must not be null");
        }
        if (federationClient == null) {
            throw new IllegalArgumentException("Federation client must not be null");
        }
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.federationClient = federationClient;
    }

    public Announcement createAnnouncement(Announcement announcement) {
        validateAnnouncementDraft(announcement);
        announcement.setActive(true);
        // Federated identity: the announcement belongs to this instance.
        announcement.setOriginInstance(FederationConfig.get().getInstanceId());

        boolean inserted;
        do {
            String generatedId = idGenerator.get();
            requireNotBlank(generatedId, "Generated announcement id must not be blank");
            announcement.setId(generatedId);
            inserted = repository.save(announcement);
        } while (!inserted);

        federationClient.broadcast(FederationEvent.announcementCreated(
                FederationConfig.get().getInstanceId(), announcement));

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
        // Preserve the home instance (the 7-arg constructor doesn't copy it).
        updatedAnnouncement.setOriginInstance(storedAnnouncement.getOriginInstance());

        if (!repository.update(updatedAnnouncement)) {
            throw new IllegalArgumentException("Announcement not found");
        }

        federationClient.broadcast(FederationEvent.announcementUpdated(
                FederationConfig.get().getInstanceId(), updatedAnnouncement));

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

        // Federation: notify peers this announcement was deleted, so replicas
        // converge instead of keeping a stale copy.
        federationClient.broadcast(FederationEvent.announcementDeleted(
                FederationConfig.get().getInstanceId(), id));

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
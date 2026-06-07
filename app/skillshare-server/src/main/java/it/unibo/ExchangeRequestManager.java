package it.unibo;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ExchangeRequestManager {

    private final ExchangeRequestRepository repository;
    private final AnnouncementRepository announcementRepository;

    public ExchangeRequestManager() {
        this(new ExchangeRequestRepository(), new AnnouncementRepository());
    }

    public ExchangeRequestManager(
            ExchangeRequestRepository repository,
            AnnouncementRepository announcementRepository) {
        if (repository == null || announcementRepository == null) {
            throw new IllegalArgumentException("Repositories must not be null");
        }
        this.repository = repository;
        this.announcementRepository = announcementRepository;
    }

    public ExchangeRequest createRequest(String announcementId, String fromUsername, String message) {
        requireNotBlank(announcementId, "Announcement id must not be blank");
        requireNotBlank(fromUsername, "Requester username must not be blank");

        Announcement announcement = announcementRepository.findById(announcementId);
        if (announcement == null || !announcement.isActive()) {
            throw new IllegalArgumentException("Announcement not found or not active");
        }

        String owner = announcement.getOwnerUsername();
        if (fromUsername.equals(owner)) {
            throw new IllegalArgumentException("You cannot request your own announcement");
        }
        if (hasPendingRequest(announcementId, fromUsername)) {
            throw new IllegalArgumentException(
                    "You already have a pending request for this announcement");
        }

        ExchangeRequest request = new ExchangeRequest(
                UUID.randomUUID().toString(),
                announcementId,
                announcement.getOfferedSkill(),
                fromUsername,
                owner,
                message == null ? "" : message.trim(),
                ExchangeRequest.STATUS_PENDING);
        repository.save(request);
        return request;
    }

    public ExchangeRequest getRequestById(String id) {
        return repository.findById(id);
    }

    public List<ExchangeRequest> getReceivedRequests(String username) {
        return repository.listAll().stream()
                .filter(r -> username != null && username.equals(r.getToUsername()))
                .collect(Collectors.toList());
    }

    public List<ExchangeRequest> getSentRequests(String username) {
        return repository.listAll().stream()
                .filter(r -> username != null && username.equals(r.getFromUsername()))
                .collect(Collectors.toList());
    }

    public boolean acceptRequest(String id) {
        return updateStatus(id, ExchangeRequest.STATUS_ACCEPTED);
    }

    public boolean rejectRequest(String id) {
        return updateStatus(id, ExchangeRequest.STATUS_REJECTED);
    }

    private boolean updateStatus(String id, String newStatus) {
        ExchangeRequest request = repository.findById(id);
        if (request == null
                || !ExchangeRequest.STATUS_PENDING.equals(request.getStatus())) {
            return false;
        }
        request.setStatus(newStatus);
        repository.update(request);
        return true;
    }

    private boolean hasPendingRequest(String announcementId, String fromUsername) {
        return repository.listAll().stream()
                .anyMatch(r -> announcementId.equals(r.getAnnouncementId())
                        && fromUsername.equals(r.getFromUsername())
                        && ExchangeRequest.STATUS_PENDING.equals(r.getStatus()));
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
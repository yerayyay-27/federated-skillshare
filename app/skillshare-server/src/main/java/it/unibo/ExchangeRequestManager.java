package it.unibo;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ExchangeRequestManager {

    private final ExchangeRequestRepository repository;
    private final AnnouncementRepository announcementRepository;
    private final FederationClient federationClient;

    public ExchangeRequestManager() {
        this(new ExchangeRequestRepository(), new AnnouncementRepository(), new FederationClient());
    }

    public ExchangeRequestManager(
            ExchangeRequestRepository repository,
            AnnouncementRepository announcementRepository) {
        this(repository, announcementRepository, new FederationClient());
    }

    // Full constructor for tests: a federation client can be injected (e.g. a
    // no-op or a capturing one) so unit tests don't perform real network calls.
    ExchangeRequestManager(
            ExchangeRequestRepository repository,
            AnnouncementRepository announcementRepository,
            FederationClient federationClient) {
        if (repository == null || announcementRepository == null) {
            throw new IllegalArgumentException("Repositories must not be null");
        }
        if (federationClient == null) {
            throw new IllegalArgumentException("Federation client must not be null");
        }
        this.repository = repository;
        this.announcementRepository = announcementRepository;
        this.federationClient = federationClient;
    }

    public ExchangeRequest createRequest(String announcementId, String fromUsername, String message) {
        requireNotBlank(announcementId, "Announcement id must not be blank");
        requireNotBlank(fromUsername, "Requester username must not be blank");

        // The announcement is found locally even when it originated on a peer,
        // because announcements are replicated to every instance.
        Announcement announcement = announcementRepository.findById(announcementId);
        if (announcement == null || !announcement.isActive()) {
            throw new IllegalArgumentException("Announcement not found or not active");
        }

        String localInstance = FederationConfig.get().getInstanceId();
        String owner = announcement.getOwnerUsername();
        // The owner's instance is the announcement's home instance. If the
        // announcement predates federation (null), it is a purely local owner.
        String ownerInstance = announcement.getOriginInstance() == null
                ? localInstance
                : announcement.getOriginInstance();

        if (sameIdentity(fromUsername, localInstance, owner, ownerInstance)) {
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
        request.setFromInstance(localInstance);
        request.setToInstance(ownerInstance);

        // Local-first: always persist locally so the requester sees it under
        // "Sent" even if the owner's instance is currently unreachable.
        repository.save(request);

        // If the owner lives on another instance, send the request there. The
        // owner's inbox will store it and show it to the owner under "Received".
        if (isRemote(ownerInstance, localInstance)) {
            federationClient.broadcast(
                    FederationEvent.exchangeRequested(localInstance, request));
        }
        return request;
    }

    public ExchangeRequest getRequestById(String id) {
        return repository.findById(id);
    }

    // Requests this instance is authoritative for (the owner lives here).
    public List<ExchangeRequest> getReceivedRequests(String username) {
        String localInstance = FederationConfig.get().getInstanceId();
        return repository.listAll().stream()
                .filter(r -> username != null && username.equals(r.getToUsername()))
                .filter(r -> belongsHere(r.getToInstance(), localInstance))
                .collect(Collectors.toList());
    }

    // Requests originated by a user of this instance (the requester lives here).
    public List<ExchangeRequest> getSentRequests(String username) {
        String localInstance = FederationConfig.get().getInstanceId();
        return repository.listAll().stream()
                .filter(r -> username != null && username.equals(r.getFromUsername()))
                .filter(r -> belongsHere(r.getFromInstance(), localInstance))
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

        // Propagate the outcome back to the requester's instance so its replica
        // converges. Done after the local update so the owner's view is never
        // blocked by an unreachable peer (availability over consistency).
        String localInstance = FederationConfig.get().getInstanceId();
        if (isRemote(request.getFromInstance(), localInstance)) {
            FederationEvent event = ExchangeRequest.STATUS_ACCEPTED.equals(newStatus)
                    ? FederationEvent.exchangeAccepted(localInstance, request)
                    : FederationEvent.exchangeRejected(localInstance, request);
            federationClient.broadcast(event);
        }
        return true;
    }

    private boolean hasPendingRequest(String announcementId, String fromUsername) {
        return repository.listAll().stream()
                .anyMatch(r -> announcementId.equals(r.getAnnouncementId())
                        && fromUsername.equals(r.getFromUsername())
                        && ExchangeRequest.STATUS_PENDING.equals(r.getStatus()));
    }

    // A request "belongs here" for display if its instance is this one, or if
    // it is null (pre-federation single-instance data).
    private boolean belongsHere(String instance, String localInstance) {
        return instance == null || instance.equals(localInstance);
    }

    private boolean isRemote(String instance, String localInstance) {
        return instance != null && !instance.equals(localInstance);
    }

    private boolean sameIdentity(
            String firstUsername,
            String firstInstance,
            String secondUsername,
            String secondInstance) {
        return firstUsername.equals(secondUsername)
                && firstInstance.equals(secondInstance);
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}

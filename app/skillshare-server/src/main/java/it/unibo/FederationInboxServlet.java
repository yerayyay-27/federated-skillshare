package it.unibo;

import java.io.BufferedReader;
import java.io.IOException;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Receives federation events from peer instances (server-to-server).
 * This is a plain Jakarta HttpServlet, not a GWT RPC servlet, because the
 * caller is another instance sending JSON over HTTP, not a browser.
 *
 * Important: when applying a received event locally, this servlet calls the
 * repository directly and does NOT go through any manager's broadcast path.
 * Otherwise receiving an event would re-broadcast it to peers, causing an
 * infinite propagation loop between instances.
 */
@SuppressWarnings("serial")
public class FederationInboxServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private final AnnouncementRepository announcementRepository;
    private final ExchangeRequestRepository exchangeRepository;
    private final FederationInboxRepository inboxRepository;

    public FederationInboxServlet() {
        this(
                new AnnouncementRepository(),
                new ExchangeRequestRepository(),
                new FederationInboxRepository());
    }

    FederationInboxServlet(
            AnnouncementRepository announcementRepository,
            ExchangeRequestRepository exchangeRepository,
            FederationInboxRepository inboxRepository) {
        if (announcementRepository == null
                || exchangeRepository == null
                || inboxRepository == null) {
            throw new IllegalArgumentException("Federation inbox repositories must not be null");
        }
        this.announcementRepository = announcementRepository;
        this.exchangeRepository = exchangeRepository;
        this.inboxRepository = inboxRepository;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = readBody(req);
        FederationEvent event;
        try {
            event = gson.fromJson(body, FederationEvent.class);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed event");
            return;
        }

        if (event == null || event.getType() == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing event type");
            return;
        }
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing event id");
            return;
        }

        applyEvent(event);

        resp.setStatus(HttpServletResponse.SC_OK);
    }

    synchronized boolean applyEvent(FederationEvent event) {
        if (event == null || event.getEventId() == null
                || event.getEventId().trim().isEmpty()) {
            throw new IllegalArgumentException("Federation event id must not be blank");
        }
        if (inboxRepository.hasProcessed(event.getEventId())) {
            System.out.println("Federation: ignored duplicate event " + event.getEventId());
            return false;
        }

        applyEventOnce(event);
        inboxRepository.markProcessed(event);
        return true;
    }

    private void applyEventOnce(FederationEvent event) {
        switch (event.getType()) {
            case FederationEvent.TYPE_ANNOUNCEMENT_CREATED:
                handleAnnouncementCreated(event);
                break;
            case FederationEvent.TYPE_ANNOUNCEMENT_UPDATED:
                handleAnnouncementUpdated(event);
                break;
            case FederationEvent.TYPE_ANNOUNCEMENT_DELETED:
                handleAnnouncementDeleted(event);
                break;
            case FederationEvent.TYPE_EXCHANGE_REQUESTED:
                handleExchangeRequested(event);
                break;
            case FederationEvent.TYPE_EXCHANGE_ACCEPTED:
                handleExchangeStatus(event, ExchangeRequest.STATUS_ACCEPTED);
                break;
            case FederationEvent.TYPE_EXCHANGE_REJECTED:
                handleExchangeStatus(event, ExchangeRequest.STATUS_REJECTED);
                break;
            default:
                // Unknown event types are ignored (forward compatibility).
                break;
        }
    }

    private void handleAnnouncementCreated(FederationEvent event) {
        Announcement remote = event.getAnnouncement();
        if (remote == null) {
            return;
        }
        // save() ignores duplicate ids atomically, so receiving the same
        // event twice is harmless (idempotent delivery).
        announcementRepository.save(remote);
        System.out.println("Federation: stored announcement " + remote.getId()
                + " from " + event.getOriginInstance());
    }

    private void handleAnnouncementUpdated(FederationEvent event) {
        Announcement remote = event.getAnnouncement();
        if (remote == null || remote.getId() == null) {
            return;
        }
        // If we already have it, update it; if we don't (e.g. the create event
        // was missed), store it so replicas still converge.
        if (!announcementRepository.update(remote)) {
            announcementRepository.save(remote);
        }
        System.out.println("Federation: updated announcement " + remote.getId()
                + " from " + event.getOriginInstance());
    }

    private void handleAnnouncementDeleted(FederationEvent event) {
        String id = event.getAnnouncementId();
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        // Deleting an already-absent announcement is a no-op, so this is
        // also idempotent.
        announcementRepository.deleteById(id);
        System.out.println("Federation: deleted announcement " + id
                + " from " + event.getOriginInstance());
    }

    // We are (or should be) the announcement owner's instance: store the new
    // request so the owner sees it under "Received". Acting only when the
    // request targets this instance keeps other peers from storing it.
    private void handleExchangeRequested(FederationEvent event) {
        ExchangeRequest remote = event.getExchangeRequest();
        if (remote == null || remote.getId() == null) {
            return;
        }
        if (!targetsThisInstance(remote.getToInstance())) {
            return;
        }
        // save() uses putIfAbsent, so a duplicate delivery is ignored.
        exchangeRepository.save(remote);
        System.out.println("Federation: stored exchange request " + remote.getId()
                + " from " + event.getOriginInstance());
    }

    // We are (or should be) the requester's instance: update our replica's
    // status so the requester sees the outcome under "Sent".
    private void handleExchangeStatus(FederationEvent event, String newStatus) {
        ExchangeRequest remote = event.getExchangeRequest();
        if (remote == null || remote.getId() == null) {
            return;
        }
        if (!targetsThisInstance(remote.getFromInstance())) {
            return;
        }
        ExchangeRequest local = exchangeRepository.findById(remote.getId());
        if (local != null) {
            local.setStatus(newStatus);
            exchangeRepository.update(local);
        } else {
            // We somehow never stored the request (e.g. missed the create);
            // store the remote copy, which already carries the right status.
            remote.setStatus(newStatus);
            exchangeRepository.save(remote);
        }
        System.out.println("Federation: applied " + newStatus
                + " to exchange request " + remote.getId()
                + " from " + event.getOriginInstance());
    }

    // True if the event is meant for this instance. Null is treated as a match
    // for backwards-compatibility, though federated events always set it.
    private boolean targetsThisInstance(String instance) {
        return instance == null
                || instance.equals(FederationConfig.get().getInstanceId());
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}

package it.unibo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.google.gson.Gson;

/**
 * Sends federation events from this instance to its known peers.
 * This is server-to-server communication (plain HTTP + JSON), distinct from
 * the GWT RPC used between the browser and its own instance.
 */
public class FederationClient {

    private static final long RETRY_DELAY_SECONDS = 60;

    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Supplier<List<String>> peerSupplier;
    private FederationOutboxRepository outboxRepository;

    public FederationClient() {
        this(() -> FederationConfig.get().getPeerUrls(), null);
    }

    FederationClient(
            Supplier<List<String>> peerSupplier,
            FederationOutboxRepository outboxRepository) {
        if (peerSupplier == null) {
            throw new IllegalArgumentException("Peer supplier must not be null");
        }
        this.peerSupplier = peerSupplier;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Broadcasts an event to every known peer. Delivery failures (e.g. a peer
     * is offline) are logged and ignored, so the local operation still
     * succeeds even if some peers are unreachable (availability over
     * immediate consistency).
     */
    public void broadcast(FederationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Federation event must not be null");
        }

        List<String> peers = peerSupplier.get();
        Instant now = Instant.now();

        for (String peer : peers) {
            if (peer == null || peer.trim().isEmpty()) {
                continue;
            }
            OutgoingFederationEvent outgoing = OutgoingFederationEvent.pending(
                    UUID.randomUUID().toString(),
                    event,
                    peer.trim(),
                    now);
            getOutboxRepository().save(outgoing);
            attemptDelivery(outgoing);
        }
    }

    /**
     * Manual retry hook for tests or future administrative code. It deliberately
     * does not start background threads or expose new HTTP endpoints.
     */
    public int retryPending() {
        List<OutgoingFederationEvent> retryable = new ArrayList<>();
        retryable.addAll(getOutboxRepository().findPending());
        retryable.addAll(getOutboxRepository().findFailed());

        int delivered = 0;
        for (OutgoingFederationEvent outgoing : retryable) {
            attemptDelivery(outgoing);
            if (FederationDeliveryStatus.DELIVERED.equals(outgoing.getStatus())) {
                delivered++;
            }
        }
        return delivered;
    }

    protected void sendToPeer(String peer, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(peer + "/federation/inbox"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOExceptionStatusException("HTTP " + response.statusCode());
        }
    }

    private void attemptDelivery(OutgoingFederationEvent outgoing) {
        String json = gson.toJson(outgoing.getEvent());
        Instant attemptedAt = Instant.now();
        try {
            sendToPeer(outgoing.getTargetPeerUrl(), json);
            outgoing.markDelivered(attemptedAt);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            Instant nextAttemptAt = attemptedAt.plusSeconds(RETRY_DELAY_SECONDS);
            outgoing.markFailed(e.getMessage(), attemptedAt, nextAttemptAt);
            // Peer unreachable: log and continue. The local action is not
            // rolled back; retryPending() can try this outbox row later.
            System.err.println("Federation: could not deliver to "
                    + outgoing.getTargetPeerUrl() + " (" + e.getMessage() + ")");
        }
        getOutboxRepository().update(outgoing);
    }

    private FederationOutboxRepository getOutboxRepository() {
        if (outboxRepository == null) {
            outboxRepository = new FederationOutboxRepository();
        }
        return outboxRepository;
    }

    private static class IOExceptionStatusException extends Exception {
        IOExceptionStatusException(String message) {
            super(message);
        }
    }
}

package it.unibo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;

/**
 * Sends federation events from this instance to its known peers.
 * This is server-to-server communication (plain HTTP + JSON), distinct from
 * the GWT RPC used between the browser and its own instance.
 */
public class FederationClient {

    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Broadcasts an event to every known peer. Delivery failures (e.g. a peer
     * is offline) are logged and ignored, so the local operation still
     * succeeds even if some peers are unreachable (availability over
     * immediate consistency).
     */
    public void broadcast(FederationEvent event) {
        List<String> peers = FederationConfig.get().getPeerUrls();
        String json = gson.toJson(event);

        for (String peer : peers) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(peer + "/federation/inbox"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                // Peer unreachable: log and continue. The local action is not
                // rolled back; convergence will be handled by later mechanisms.
                System.err.println("Federation: could not deliver to " + peer + " (" + e.getMessage() + ")");
            }
        }
    }
}
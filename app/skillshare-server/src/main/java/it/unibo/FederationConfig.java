package it.unibo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-instance configuration for the federated deployment.
 * Each Skillshare instance reads its own identity and the list of peer
 * instances it federates with, from environment variables (or system
 * properties as a fallback). This keeps the same code runnable as any
 * instance, configured only from the outside.
 *
 * Environment variables:
 *   INSTANCE_ID   - this instance's identifier, e.g. "inst-a"
 *   INSTANCE_URL  - this instance's own base URL, e.g. "http://localhost:8080"
 *   PEER_URLS     - comma-separated base URLs of known peer instances,
 *                   e.g. "http://localhost:8081,http://localhost:8082"
 */
public class FederationConfig {

    private final String instanceId;
    private final String instanceUrl;
    private final List<String> peerUrls;

    private static FederationConfig instance;

    private FederationConfig() {
        this.instanceId = read("INSTANCE_ID", "inst-local");
        this.instanceUrl = read("INSTANCE_URL", "http://localhost:8080");
        this.peerUrls = parsePeers(read("PEER_URLS", ""));
    }

    public static synchronized FederationConfig get() {
        if (instance == null) {
            instance = new FederationConfig();
        }
        return instance;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public List<String> getPeerUrls() {
        return Collections.unmodifiableList(peerUrls);
    }

    // Reads an environment variable, falling back to a system property,
    // then to a default value.
    private static String read(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    private static List<String> parsePeers(String raw) {
        List<String> peers = new ArrayList<>();
        if (raw != null) {
            for (String url : raw.split(",")) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) {
                    peers.add(trimmed);
                }
            }
        }
        return peers;
    }
}
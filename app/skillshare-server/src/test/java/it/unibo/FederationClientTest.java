package it.unibo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FederationClientTest {

    private FederationOutboxRepository repository;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        repository = new FederationOutboxRepository();
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void successfulSendMarksEventDelivered() {
        TestFederationClient client = new TestFederationClient(
                repository,
                new String[] { "http://peer-a:8080" });

        client.broadcast(event());

        assertEquals(1, repository.findAll().size());
        OutgoingFederationEvent stored = repository.findAll().get(0);
        assertEquals(FederationDeliveryStatus.DELIVERED, stored.getStatus());
        assertEquals(1, stored.getAttemptCount());
        assertEquals(1, client.getSendCount());
    }

    @Test
    void failedSendStoresFailedEvent() {
        TestFederationClient client = new TestFederationClient(
                repository,
                new String[] { "http://peer-a:8080" });
        client.failPeer("http://peer-a:8080");

        client.broadcast(event());

        assertEquals(1, repository.findFailed().size());
        OutgoingFederationEvent failed = repository.findFailed().get(0);
        assertEquals(FederationDeliveryStatus.FAILED, failed.getStatus());
        assertEquals(1, failed.getAttemptCount());
        assertTrue(failed.getLastError().contains("offline"));
    }

    @Test
    void retryPendingCanMarkFailedEventDeliveredLater() {
        TestFederationClient client = new TestFederationClient(
                repository,
                new String[] { "http://peer-a:8080" });
        client.failPeer("http://peer-a:8080");
        client.broadcast(event());
        client.recoverPeer("http://peer-a:8080");

        int delivered = client.retryPending();

        assertEquals(1, delivered);
        assertEquals(1, repository.findAll().size());
        OutgoingFederationEvent stored = repository.findAll().get(0);
        assertEquals(FederationDeliveryStatus.DELIVERED, stored.getStatus());
        assertEquals(2, stored.getAttemptCount());
    }

    @Test
    void onePeerFailureDoesNotPreventOtherDeliveries() {
        TestFederationClient client = new TestFederationClient(
                repository,
                new String[] { "http://peer-a:8080", "http://peer-b:8080" });
        client.failPeer("http://peer-a:8080");

        assertDoesNotThrow(() -> client.broadcast(event()));

        assertEquals(2, repository.findAll().size());
        assertEquals(1, repository.findFailed().size());
        long delivered = repository.findAll().stream()
                .filter(e -> FederationDeliveryStatus.DELIVERED.equals(e.getStatus()))
                .count();
        assertEquals(1, delivered);
    }

    private FederationEvent event() {
        Announcement announcement = new Announcement(
                "ann-1",
                "alice",
                "Java",
                "Spanish",
                "Basics",
                "Evenings",
                true);
        announcement.setOriginInstance("inst-a");
        return FederationEvent.announcementCreated("inst-a", announcement);
    }

    private static class TestFederationClient extends FederationClient {

        private final Set<String> failedPeers = new HashSet<>();
        private int sendCount;

        TestFederationClient(FederationOutboxRepository repository, String[] peers) {
            super(() -> Arrays.asList(peers), repository);
        }

        void failPeer(String peer) {
            failedPeers.add(peer);
        }

        void recoverPeer(String peer) {
            failedPeers.remove(peer);
        }

        int getSendCount() {
            return sendCount;
        }

        @Override
        protected void sendToPeer(String peer, String json) throws Exception {
            sendCount++;
            if (failedPeers.contains(peer)) {
                throw new Exception("offline " + peer);
            }
        }
    }
}

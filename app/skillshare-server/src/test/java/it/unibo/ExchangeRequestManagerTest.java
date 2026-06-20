package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExchangeRequestManagerTest {

    private static final String LOCAL_INSTANCE = "inst-local"; // FederationConfig default
    private static final String REMOTE_INSTANCE = "inst-b";

    private AnnouncementRepository announcementRepository;
    private ExchangeRequestRepository exchangeRepository;
    private CapturingFederationClient federation;
    private ExchangeRequestManager manager;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        announcementRepository = new AnnouncementRepository();
        exchangeRepository = new ExchangeRequestRepository();
        federation = new CapturingFederationClient();
        manager = new ExchangeRequestManager(
                exchangeRepository, announcementRepository, federation);
        // Local announcement (no origin instance set -> treated as local owner).
        announcementRepository.save(new Announcement(
                "ann-1", "alice", "Java tutoring", "Spanish",
                "I teach Java.", "Evenings", true));
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void createValidRequest() {
        ExchangeRequest request = manager.createRequest("ann-1", "bob", "Hi!");

        assertEquals("bob", request.getFromUsername());
        assertEquals("alice", request.getToUsername());
        assertEquals(ExchangeRequest.STATUS_PENDING, request.getStatus());
    }

    @Test
    void rejectRequestForOwnAnnouncement() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.createRequest("ann-1", "alice", "x"));
    }

    @Test
    void rejectRequestForUnknownAnnouncement() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.createRequest("ghost", "bob", "x"));
    }

    @Test
    void rejectDuplicatePendingRequest() {
        manager.createRequest("ann-1", "bob", "first");

        assertThrows(IllegalArgumentException.class,
                () -> manager.createRequest("ann-1", "bob", "second"));
    }

    @Test
    void getReceivedRequests() {
        manager.createRequest("ann-1", "bob", "x");

        List<ExchangeRequest> received = manager.getReceivedRequests("alice");

        assertEquals(1, received.size());
        assertEquals("bob", received.get(0).getFromUsername());
    }

    @Test
    void getSentRequests() {
        manager.createRequest("ann-1", "bob", "x");

        List<ExchangeRequest> sent = manager.getSentRequests("bob");

        assertEquals(1, sent.size());
        assertEquals("alice", sent.get(0).getToUsername());
    }

    @Test
    void acceptPendingRequest() {
        ExchangeRequest request = manager.createRequest("ann-1", "bob", "x");

        assertTrue(manager.acceptRequest(request.getId()));
        assertEquals(ExchangeRequest.STATUS_ACCEPTED,
                manager.getRequestById(request.getId()).getStatus());
    }

    @Test
    void rejectPendingRequest() {
        ExchangeRequest request = manager.createRequest("ann-1", "bob", "x");

        assertTrue(manager.rejectRequest(request.getId()));
        assertEquals(ExchangeRequest.STATUS_REJECTED,
                manager.getRequestById(request.getId()).getStatus());
    }

    @Test
    void cannotAcceptAlreadyHandledRequest() {
        ExchangeRequest request = manager.createRequest("ann-1", "bob", "x");
        manager.acceptRequest(request.getId());

        assertFalse(manager.acceptRequest(request.getId()));
    }

    @Test
    void acceptUnknownRequestReturnsFalse() {
        assertFalse(manager.acceptRequest("ghost"));
    }

    // --- Federation-specific behaviour ---

    @Test
    void localRequestDoesNotBroadcast() {
        manager.createRequest("ann-1", "bob", "x");

        assertTrue(federation.events.isEmpty());
    }

    @Test
    void requestOnRemoteAnnouncementStampsInstancesAndBroadcasts() {
        // A replica of an announcement that lives on another instance.
        Announcement remoteAnnouncement = new Announcement(
                "ann-2", "carol", "Piano", "Cooking", "Lessons", "Weekends", true);
        remoteAnnouncement.setOriginInstance(REMOTE_INSTANCE);
        announcementRepository.save(remoteAnnouncement);

        ExchangeRequest request = manager.createRequest("ann-2", "bob", "hello");

        assertEquals(LOCAL_INSTANCE, request.getFromInstance());
        assertEquals(REMOTE_INSTANCE, request.getToInstance());
        assertEquals(1, federation.events.size());
        FederationEvent event = federation.events.get(0);
        assertEquals(FederationEvent.TYPE_EXCHANGE_REQUESTED, event.getType());
        assertEquals(request.getId(), event.getExchangeRequest().getId());
    }

    @Test
    void acceptingRemoteRequesterBroadcastsAccepted() {
        // This instance is the owner; the requester lives on a remote instance.
        ExchangeRequest inbound = new ExchangeRequest(
                "ex-remote", "ann-1", "Java tutoring", "bob", "alice", "hi",
                ExchangeRequest.STATUS_PENDING);
        inbound.setFromInstance(REMOTE_INSTANCE);
        inbound.setToInstance(LOCAL_INSTANCE);
        exchangeRepository.save(inbound);

        assertTrue(manager.acceptRequest("ex-remote"));

        assertEquals(1, federation.events.size());
        assertEquals(FederationEvent.TYPE_EXCHANGE_ACCEPTED,
                federation.events.get(0).getType());
    }

    @Test
    void receivedExcludesRequestsOwnedByAnotherInstance() {
        // Stored locally as a "Sent" replica: the owner is on a remote instance.
        ExchangeRequest sentReplica = new ExchangeRequest(
                "ex-sent", "ann-2", "Piano", "bob", "carol", "hi",
                ExchangeRequest.STATUS_PENDING);
        sentReplica.setFromInstance(LOCAL_INSTANCE);
        sentReplica.setToInstance(REMOTE_INSTANCE);
        exchangeRepository.save(sentReplica);

        // It must show under bob's "Sent", not under carol's "Received" here.
        assertEquals(1, manager.getSentRequests("bob").size());
        assertTrue(manager.getReceivedRequests("carol").isEmpty());
    }

    private static class CapturingFederationClient extends FederationClient {
        final List<FederationEvent> events = new ArrayList<>();

        @Override
        public void broadcast(FederationEvent event) {
            events.add(event);
        }
    }
}
package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExchangeRequestManagerTest {

    private AnnouncementRepository announcementRepository;
    private ExchangeRequestManager manager;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        announcementRepository = new AnnouncementRepository();
        manager = new ExchangeRequestManager(
                new ExchangeRequestRepository(), announcementRepository);
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
}
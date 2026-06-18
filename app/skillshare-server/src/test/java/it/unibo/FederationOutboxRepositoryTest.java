package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FederationOutboxRepositoryTest {

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
    void saveAndFind() {
        OutgoingFederationEvent outgoing = outgoing("out-1", "http://peer-a:8080");

        assertTrue(repository.save(outgoing));

        OutgoingFederationEvent stored = repository.findById("out-1");
        assertNotNull(stored);
        assertEquals("http://peer-a:8080", stored.getTargetPeerUrl());
    }

    @Test
    void rejectDuplicateSave() {
        repository.save(outgoing("out-1", "http://peer-a:8080"));

        assertFalse(repository.save(outgoing("out-1", "http://peer-b:8080")));
    }

    @Test
    void listAll() {
        repository.save(outgoing("out-1", "http://peer-a:8080"));
        repository.save(outgoing("out-2", "http://peer-b:8080"));

        List<OutgoingFederationEvent> all = repository.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void findPendingAndFailed() {
        OutgoingFederationEvent pending = outgoing("out-1", "http://peer-a:8080");
        OutgoingFederationEvent failed = outgoing("out-2", "http://peer-b:8080");
        failed.markFailed("offline", Instant.now(), Instant.now());
        OutgoingFederationEvent delivered = outgoing("out-3", "http://peer-c:8080");
        delivered.markDelivered(Instant.now());
        repository.save(pending);
        repository.save(failed);
        repository.save(delivered);

        assertEquals(1, repository.findPending().size());
        assertEquals("out-1", repository.findPending().get(0).getId());
        assertEquals(1, repository.findFailed().size());
        assertEquals("out-2", repository.findFailed().get(0).getId());
    }

    @Test
    void updateDelivered() {
        OutgoingFederationEvent outgoing = outgoing("out-1", "http://peer-a:8080");
        repository.save(outgoing);
        outgoing.markDelivered(Instant.now());

        repository.update(outgoing);

        assertEquals(
                FederationDeliveryStatus.DELIVERED,
                repository.findById("out-1").getStatus());
    }

    @Test
    void upsertCreatesOrReplaces() {
        OutgoingFederationEvent outgoing = outgoing("out-1", "http://peer-a:8080");
        repository.upsert(outgoing);
        outgoing.markFailed("offline", Instant.now(), Instant.now());

        repository.upsert(outgoing);

        assertEquals(FederationDeliveryStatus.FAILED, repository.findById("out-1").getStatus());
    }

    @Test
    void deleteExisting() {
        repository.save(outgoing("out-1", "http://peer-a:8080"));

        assertTrue(repository.delete("out-1"));
        assertEquals(null, repository.findById("out-1"));
    }

    @Test
    void deleteUnknownReturnsFalse() {
        assertFalse(repository.delete("unknown"));
    }

    private OutgoingFederationEvent outgoing(String id, String peer) {
        return OutgoingFederationEvent.pending(
                id,
                FederationEvent.announcementCreated("inst-a", announcement()),
                peer,
                Instant.parse("2026-01-01T10:00:00Z"));
    }

    private Announcement announcement() {
        Announcement announcement = new Announcement(
                "ann-1",
                "alice",
                "Java",
                "Spanish",
                "Basics",
                "Evenings",
                true);
        announcement.setOriginInstance("inst-a");
        return announcement;
    }
}

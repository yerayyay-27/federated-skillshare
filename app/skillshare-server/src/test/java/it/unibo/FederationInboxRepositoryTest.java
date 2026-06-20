package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FederationInboxRepositoryTest {

    private FederationInboxRepository repository;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        repository = new FederationInboxRepository();
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void markProcessedStoresEventMetadata() {
        FederationEvent event = event();

        assertTrue(repository.markProcessed(event));

        assertTrue(repository.hasProcessed(event.getEventId()));
        ProcessedFederationEvent stored = repository.findById(event.getEventId());
        assertNotNull(stored);
        assertEquals(event.getType(), stored.getEventType());
        assertEquals(event.getOriginInstance(), stored.getOriginInstance());
        assertNotNull(stored.getProcessedAt());
    }

    @Test
    void unknownEventHasNotBeenProcessed() {
        assertFalse(repository.hasProcessed("unknown-event"));
    }

    @Test
    void duplicateMarkIsSafe() {
        FederationEvent event = event();

        assertTrue(repository.markProcessed(event));
        assertFalse(repository.markProcessed(event));

        assertEquals(1, repository.findAll().size());
    }

    private FederationEvent event() {
        return FederationEvent.announcementDeleted("inst-a", "ann-1");
    }
}

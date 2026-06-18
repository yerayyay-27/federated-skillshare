package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OutgoingFederationEventTest {

    @Test
    void createValidPendingEvent() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");

        OutgoingFederationEvent outgoing = OutgoingFederationEvent.pending(
                "out-1",
                event(),
                "http://peer:8080",
                createdAt);

        assertEquals("out-1", outgoing.getId());
        assertEquals("http://peer:8080", outgoing.getTargetPeerUrl());
        assertEquals(FederationDeliveryStatus.PENDING, outgoing.getStatus());
        assertEquals(0, outgoing.getAttemptCount());
        assertEquals(createdAt, outgoing.getCreatedAt());
        assertNull(outgoing.getLastAttemptAt());
    }

    @Test
    void rejectNullEvent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OutgoingFederationEvent.pending(
                        "out-1",
                        null,
                        "http://peer:8080",
                        Instant.now()));
    }

    @Test
    void rejectBlankPeer() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OutgoingFederationEvent.pending(
                        "out-1",
                        event(),
                        " ",
                        Instant.now()));
    }

    @Test
    void rejectNullStatus() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutgoingFederationEvent(
                        "out-1",
                        event(),
                        "http://peer:8080",
                        null,
                        0,
                        Instant.now(),
                        null,
                        null,
                        null));
    }

    @Test
    void rejectNegativeAttemptCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutgoingFederationEvent(
                        "out-1",
                        event(),
                        "http://peer:8080",
                        FederationDeliveryStatus.PENDING,
                        -1,
                        Instant.now(),
                        null,
                        null,
                        null));
    }

    @Test
    void markDeliveredClearsErrorAndNextAttempt() {
        OutgoingFederationEvent outgoing = OutgoingFederationEvent.pending(
                "out-1",
                event(),
                "http://peer:8080",
                Instant.now());
        outgoing.markFailed(
                "offline",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:01:00Z"));

        outgoing.markDelivered(Instant.parse("2026-01-01T10:02:00Z"));

        assertEquals(FederationDeliveryStatus.DELIVERED, outgoing.getStatus());
        assertEquals(2, outgoing.getAttemptCount());
        assertEquals(Instant.parse("2026-01-01T10:02:00Z"), outgoing.getLastAttemptAt());
        assertNull(outgoing.getNextAttemptAt());
        assertNull(outgoing.getLastError());
    }

    @Test
    void markFailedStoresErrorAndRetryTime() {
        OutgoingFederationEvent outgoing = OutgoingFederationEvent.pending(
                "out-1",
                event(),
                "http://peer:8080",
                Instant.now());
        Instant attemptedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant nextAttemptAt = Instant.parse("2026-01-01T10:01:00Z");

        outgoing.markFailed("offline", attemptedAt, nextAttemptAt);

        assertEquals(FederationDeliveryStatus.FAILED, outgoing.getStatus());
        assertEquals(1, outgoing.getAttemptCount());
        assertEquals(attemptedAt, outgoing.getLastAttemptAt());
        assertEquals(nextAttemptAt, outgoing.getNextAttemptAt());
        assertEquals("offline", outgoing.getLastError());
    }

    @Test
    void markPendingResetsRetryState() {
        OutgoingFederationEvent outgoing = OutgoingFederationEvent.pending(
                "out-1",
                event(),
                "http://peer:8080",
                Instant.now());
        outgoing.markFailed("offline", Instant.now(), Instant.now());

        outgoing.markPending();

        assertEquals(FederationDeliveryStatus.PENDING, outgoing.getStatus());
        assertNull(outgoing.getNextAttemptAt());
        assertNull(outgoing.getLastError());
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
}

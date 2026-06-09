package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReviewManagerTest {

    private ExchangeRequestRepository exchangeRepository;
    private ReviewManager manager;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        exchangeRepository = new ExchangeRequestRepository();
        manager = new ReviewManager(new ReviewRepository(), exchangeRepository);
        // accepted exchange: bob (requester) <-> alice (owner)
        exchangeRepository.save(new ExchangeRequest(
                "ex-1", "ann-1", "Java tutoring", "bob", "alice", "hi",
                ExchangeRequest.STATUS_ACCEPTED));
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void createReviewTargetsTheOtherParticipant() {
        Review review = manager.createReview("ex-1", "bob", 5, "Great teacher");

        assertEquals("bob", review.getFromUsername());
        assertEquals("alice", review.getToUsername());
        assertEquals(5, review.getRating());
    }

    @Test
    void rejectRatingOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.createReview("ex-1", "bob", 0, "x"));
        assertThrows(IllegalArgumentException.class,
                () -> manager.createReview("ex-1", "bob", 6, "x"));
    }

    @Test
    void rejectNonParticipant() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.createReview("ex-1", "carol", 4, "x"));
    }

    @Test
    void rejectReviewOnNonAcceptedExchange() {
        exchangeRepository.save(new ExchangeRequest(
                "ex-2", "ann-2", "Piano", "bob", "alice", "hi",
                ExchangeRequest.STATUS_PENDING));

        assertThrows(IllegalArgumentException.class,
                () -> manager.createReview("ex-2", "bob", 4, "x"));
    }

    @Test
    void rejectDoubleReviewFromSameUser() {
        manager.createReview("ex-1", "bob", 5, "first");

        assertThrows(IllegalArgumentException.class,
                () -> manager.createReview("ex-1", "bob", 3, "second"));
    }

    @Test
    void bothParticipantsCanReviewEachOther() {
        manager.createReview("ex-1", "bob", 5, "great");
        manager.createReview("ex-1", "alice", 4, "good student");

        assertEquals(1, manager.getReputation("alice").getReviewCount());
        assertEquals(1, manager.getReputation("bob").getReviewCount());
    }

    @Test
    void computeAverageRating() {
        // two accepted exchanges so bob can review alice twice (different exchanges)
        exchangeRepository.save(new ExchangeRequest(
                "ex-3", "ann-3", "Cooking", "bob", "alice", "hi",
                ExchangeRequest.STATUS_ACCEPTED));
        manager.createReview("ex-1", "bob", 5, "a");
        manager.createReview("ex-3", "bob", 3, "b");

        UserReputation reputation = manager.getReputation("alice");

        assertEquals(2, reputation.getReviewCount());
        assertEquals(4.0, reputation.getAverageRating(), 0.001);
    }

    @Test
    void reputationEmptyWhenNoReviews() {
        UserReputation reputation = manager.getReputation("nobody");

        assertEquals(0, reputation.getReviewCount());
        assertEquals(0.0, reputation.getAverageRating(), 0.001);
    }

    @Test
    void noBlockReasonForParticipantWithoutReview() {
        assertNull(manager.getReviewBlockReason("ex-1", "bob"));
    }

    @Test
    void blockReasonAfterReviewing() {
        manager.createReview("ex-1", "bob", 5, "x");
        assertNotNull(manager.getReviewBlockReason("ex-1", "bob"));
    }
}
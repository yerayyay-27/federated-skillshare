package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReviewTest {

    @Test
    void localReviewWithoutInstancesKeepsLegacyHandles() {
        Review review = review();

        assertEquals("alice", review.getFromHandle());
        assertEquals("bob", review.getToHandle());
    }

    @Test
    void federatedReviewDisplaysDistinctHandles() {
        Review review = review();
        review.setFromInstance("inst-a");
        review.setToInstance("inst-b");

        assertEquals("alice@inst-a", review.getFromHandle());
        assertEquals("bob@inst-b", review.getToHandle());
    }

    @Test
    void sameUsernameOnDifferentInstancesHasDifferentHandles() {
        Review review = review();
        review.setToUsername("alice");
        review.setFromInstance("inst-a");
        review.setToInstance("inst-b");

        assertEquals("alice@inst-a", review.getFromHandle());
        assertEquals("alice@inst-b", review.getToHandle());
    }

    private Review review() {
        return new Review(
                "review-1", "exchange-1", "alice", "bob",
                5, "Great", System.currentTimeMillis());
    }
}

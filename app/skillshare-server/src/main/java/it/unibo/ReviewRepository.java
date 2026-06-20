package it.unibo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.Serializer;

public class ReviewRepository {

    private final ConcurrentMap<String, Review> reviews;

    @SuppressWarnings("unchecked")
    public ReviewRepository() {
        DB db = DatabaseCore.getDB();
        reviews = (ConcurrentMap<String, Review>) (ConcurrentMap<?, ?>) db
                .hashMap("reviews", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();
    }

    public boolean save(Review review) {
        if (review == null || review.getId() == null || review.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Review and review id must not be blank");
        }
        Review existing = reviews.putIfAbsent(review.getId(), review);
        if (existing != null) {
            return false;
        }
        DatabaseCore.commit();
        return true;
    }

    public List<Review> listAll() {
        return new ArrayList<>(reviews.values());
    }

    public List<Review> findByToUsername(String username) {
        List<Review> result = new ArrayList<>();
        for (Review review : reviews.values()) {
            if (username != null && username.equals(review.getToUsername())) {
                result.add(review);
            }
        }
        return result;
    }

    public List<Review> findByReviewedIdentity(String username, String instance) {
        List<Review> result = new ArrayList<>();
        for (Review review : reviews.values()) {
            if (username != null
                    && username.equals(review.getToUsername())
                    && belongsToInstance(review.getToInstance(), instance)) {
                result.add(review);
            }
        }
        return result;
    }

    // true if this user has already reviewed within this exchange
    public boolean existsForExchangeFrom(String exchangeRequestId, String fromUsername) {
        for (Review review : reviews.values()) {
            if (exchangeRequestId.equals(review.getExchangeRequestId())
                    && fromUsername.equals(review.getFromUsername())) {
                return true;
            }
        }
        return false;
    }

    public boolean existsForExchangeFrom(
            String exchangeRequestId,
            String fromUsername,
            String fromInstance) {
        for (Review review : reviews.values()) {
            if (exchangeRequestId.equals(review.getExchangeRequestId())
                    && fromUsername.equals(review.getFromUsername())
                    && belongsToInstance(review.getFromInstance(), fromInstance)) {
                return true;
            }
        }
        return false;
    }

    private boolean belongsToInstance(String storedInstance, String expectedInstance) {
        String effectiveStoredInstance = storedInstance == null
                ? FederationConfig.get().getInstanceId()
                : storedInstance;
        return effectiveStoredInstance.equals(expectedInstance);
    }
}

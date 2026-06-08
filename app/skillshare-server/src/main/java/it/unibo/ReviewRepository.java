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

    public void save(Review review) {
        reviews.put(review.getId(), review);
        DatabaseCore.commit();
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
}
package it.unibo;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class ReviewServiceImpl extends RemoteServiceServlet implements ReviewService {

    private final ReviewManager reviewManager;

    public ReviewServiceImpl() {
        this(new ReviewManager());
    }

    ReviewServiceImpl(ReviewManager reviewManager) {
        if (reviewManager == null) {
            throw new IllegalArgumentException("Review manager must not be null");
        }
        this.reviewManager = reviewManager;
    }

    @Override
    public Review createReview(String exchangeRequestId, String fromUsername,
            int rating, String comment) throws IllegalArgumentException {
        return reviewManager.createReview(exchangeRequestId, fromUsername, rating, comment);
    }

    @Override
    public UserReputation getReputation(String username) {
        return reviewManager.getReputation(username);
    }

    @Override
    public String getReviewBlockReason(String exchangeRequestId, String fromUsername) {
        return reviewManager.getReviewBlockReason(exchangeRequestId, fromUsername);
    }
}
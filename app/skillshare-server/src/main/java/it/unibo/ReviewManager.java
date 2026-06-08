package it.unibo;

import java.util.List;
import java.util.UUID;

public class ReviewManager {

    private final ReviewRepository reviewRepository;
    private final ExchangeRequestRepository exchangeRepository;

    public ReviewManager() {
        this(new ReviewRepository(), new ExchangeRequestRepository());
    }

    public ReviewManager(ReviewRepository reviewRepository,
            ExchangeRequestRepository exchangeRepository) {
        if (reviewRepository == null || exchangeRepository == null) {
            throw new IllegalArgumentException("Repositories must not be null");
        }
        this.reviewRepository = reviewRepository;
        this.exchangeRepository = exchangeRepository;
    }

    public Review createReview(String exchangeRequestId, String fromUsername,
            int rating, String comment) {
        requireNotBlank(exchangeRequestId, "Exchange request id must not be blank");
        requireNotBlank(fromUsername, "Reviewer username must not be blank");
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        ExchangeRequest request = exchangeRepository.findById(exchangeRequestId);
        String target = resolveTarget(request, fromUsername);

        if (reviewRepository.existsForExchangeFrom(exchangeRequestId, fromUsername)) {
            throw new IllegalArgumentException("You have already reviewed this exchange");
        }

        Review review = new Review(
                UUID.randomUUID().toString(),
                exchangeRequestId,
                fromUsername,
                target,
                rating,
                comment == null ? "" : comment.trim(),
                System.currentTimeMillis());
        reviewRepository.save(review);
        return review;
    }

    public UserReputation getReputation(String username) {
        List<Review> received = reviewRepository.findByToUsername(username);
        int count = received.size();
        double average = 0.0;
        if (count > 0) {
            int sum = 0;
            for (Review review : received) {
                sum += review.getRating();
            }
            average = (double) sum / count;
        }
        return new UserReputation(username, average, count, received);
    }

    public boolean canReview(String exchangeRequestId, String fromUsername) {
        ExchangeRequest request = exchangeRepository.findById(exchangeRequestId);
        if (request == null
                || !ExchangeRequest.STATUS_ACCEPTED.equals(request.getStatus())
                || !isParticipant(request, fromUsername)) {
            return false;
        }
        return !reviewRepository.existsForExchangeFrom(exchangeRequestId, fromUsername);
    }

    // Validates the exchange is accepted and the reviewer is a participant,
    // and returns the OTHER participant (the one being reviewed).
    private String resolveTarget(ExchangeRequest request, String fromUsername) {
        if (request == null) {
            throw new IllegalArgumentException("Exchange request not found");
        }
        if (!ExchangeRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
            throw new IllegalArgumentException("You can only review an accepted exchange");
        }
        if (fromUsername.equals(request.getFromUsername())) {
            return request.getToUsername();
        }
        if (fromUsername.equals(request.getToUsername())) {
            return request.getFromUsername();
        }
        throw new IllegalArgumentException("You are not a participant of this exchange");
    }

    private boolean isParticipant(ExchangeRequest request, String username) {
        return username != null
                && (username.equals(request.getFromUsername())
                        || username.equals(request.getToUsername()));
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
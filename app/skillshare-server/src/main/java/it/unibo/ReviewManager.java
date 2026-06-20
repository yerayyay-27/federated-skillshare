package it.unibo;

import java.util.List;
import java.util.UUID;

public class ReviewManager {

    private final ReviewRepository reviewRepository;
    private final ExchangeRequestRepository exchangeRepository;
    private final FederationClient federationClient;

    public ReviewManager() {
        this(new ReviewRepository(), new ExchangeRequestRepository(), new FederationClient());
    }

    public ReviewManager(ReviewRepository reviewRepository,
            ExchangeRequestRepository exchangeRepository) {
        this(reviewRepository, exchangeRepository, new FederationClient());
    }

    ReviewManager(
            ReviewRepository reviewRepository,
            ExchangeRequestRepository exchangeRepository,
            FederationClient federationClient) {
        if (reviewRepository == null || exchangeRepository == null) {
            throw new IllegalArgumentException("Repositories must not be null");
        }
        if (federationClient == null) {
            throw new IllegalArgumentException("Federation client must not be null");
        }
        this.reviewRepository = reviewRepository;
        this.exchangeRepository = exchangeRepository;
        this.federationClient = federationClient;
    }

    public Review createReview(String exchangeRequestId, String fromUsername,
            int rating, String comment) {
        requireNotBlank(exchangeRequestId, "Exchange request id must not be blank");
        requireNotBlank(fromUsername, "Reviewer username must not be blank");
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        ExchangeRequest request = exchangeRepository.findById(exchangeRequestId);
        String localInstance = FederationConfig.get().getInstanceId();
        Participant target = resolveTarget(request, fromUsername, localInstance);

        if (reviewRepository.existsForExchangeFrom(
                exchangeRequestId, fromUsername, localInstance)) {
            throw new IllegalArgumentException("You have already reviewed this exchange");
        }

        Review review = new Review(
                UUID.randomUUID().toString(),
                exchangeRequestId,
                fromUsername,
                target.username,
                rating,
                comment == null ? "" : comment.trim(),
                System.currentTimeMillis());
        review.setFromInstance(localInstance);
        review.setToInstance(target.instance);
        reviewRepository.save(review);

        if (!localInstance.equals(target.instance)) {
            federationClient.broadcast(FederationEvent.reviewCreated(localInstance, review));
        }
        return review;
    }

    public UserReputation getReputation(String username) {
        List<Review> received = reviewRepository.findByReviewedIdentity(
                username, FederationConfig.get().getInstanceId());
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

    // Returns null if the user can review, otherwise a human-readable reason why not.
    public String getReviewBlockReason(String exchangeRequestId, String fromUsername) {
        ExchangeRequest request = exchangeRepository.findById(exchangeRequestId);
        if (request == null) {
            return "This exchange no longer exists.";
        }
        if (!ExchangeRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
            return "You can only review an accepted exchange.";
        }
        String localInstance = FederationConfig.get().getInstanceId();
        if (!isParticipant(request, fromUsername, localInstance)) {
            return "You are not a participant of this exchange.";
        }
        if (reviewRepository.existsForExchangeFrom(
                exchangeRequestId, fromUsername, localInstance)) {
            return "You have already reviewed this exchange. You can only leave one review per exchange.";
        }
        return null; // can review
    }

    // Validates the exchange is accepted and the reviewer is a participant,
    // and returns the OTHER participant (the one being reviewed).
    private Participant resolveTarget(
            ExchangeRequest request,
            String fromUsername,
            String localInstance) {
        if (request == null) {
            throw new IllegalArgumentException("Exchange request not found");
        }
        if (!ExchangeRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
            throw new IllegalArgumentException("You can only review an accepted exchange");
        }
        String fromInstance = orLocal(request.getFromInstance(), localInstance);
        String toInstance = orLocal(request.getToInstance(), localInstance);
        if (sameIdentity(fromUsername, localInstance,
                request.getFromUsername(), fromInstance)) {
            return otherParticipant(
                    fromUsername, localInstance, request.getToUsername(), toInstance);
        }
        if (sameIdentity(fromUsername, localInstance,
                request.getToUsername(), toInstance)) {
            return otherParticipant(
                    fromUsername, localInstance, request.getFromUsername(), fromInstance);
        }
        throw new IllegalArgumentException("You are not a participant of this exchange");
    }

    private Participant otherParticipant(
            String reviewerUsername,
            String reviewerInstance,
            String targetUsername,
            String targetInstance) {
        if (sameIdentity(reviewerUsername, reviewerInstance, targetUsername, targetInstance)) {
            throw new IllegalArgumentException("You cannot review yourself");
        }
        return new Participant(targetUsername, targetInstance);
    }

    private boolean isParticipant(
            ExchangeRequest request,
            String username,
            String localInstance) {
        if (username == null) {
            return false;
        }
        return sameIdentity(username, localInstance,
                    request.getFromUsername(), orLocal(request.getFromInstance(), localInstance))
                || sameIdentity(username, localInstance,
                    request.getToUsername(), orLocal(request.getToInstance(), localInstance));
    }

    private String orLocal(String instance, String localInstance) {
        return instance == null ? localInstance : instance;
    }

    private boolean sameIdentity(
            String firstUsername,
            String firstInstance,
            String secondUsername,
            String secondInstance) {
        return firstUsername.equals(secondUsername)
                && firstInstance.equals(secondInstance);
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static class Participant {
        private final String username;
        private final String instance;

        Participant(String username, String instance) {
            this.username = username;
            this.instance = instance;
        }
    }
}

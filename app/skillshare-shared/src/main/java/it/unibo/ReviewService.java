package it.unibo;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("reviews")
public interface ReviewService extends RemoteService {

    Review createReview(String exchangeRequestId, String fromUsername, int rating, String comment)
            throws IllegalArgumentException;

    UserReputation getReputation(String username);

    String getReviewBlockReason(String exchangeRequestId, String fromUsername);
}
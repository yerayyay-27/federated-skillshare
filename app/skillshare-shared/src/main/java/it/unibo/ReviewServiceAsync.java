package it.unibo;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ReviewServiceAsync {

    void createReview(String exchangeRequestId, String fromUsername, int rating, String comment,
            AsyncCallback<Review> callback);

    void getReputation(String username, AsyncCallback<UserReputation> callback);

    void canReview(String exchangeRequestId, String fromUsername, AsyncCallback<Boolean> callback);
}
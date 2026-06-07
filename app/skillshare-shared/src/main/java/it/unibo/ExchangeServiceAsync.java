package it.unibo;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ExchangeServiceAsync {

    void createRequest(String announcementId, String fromUsername, String message,
            AsyncCallback<ExchangeRequest> callback);

    void getReceivedRequests(String username, AsyncCallback<List<ExchangeRequest>> callback);

    void getSentRequests(String username, AsyncCallback<List<ExchangeRequest>> callback);

    void acceptRequest(String id, AsyncCallback<Boolean> callback);

    void rejectRequest(String id, AsyncCallback<Boolean> callback);
}
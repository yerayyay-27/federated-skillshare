package it.unibo;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ChatServiceAsync {

    void sendMessage(String exchangeRequestId, String senderUsername, String text,
            AsyncCallback<ChatMessage> callback);

    void getMessages(String exchangeRequestId, String username,
            AsyncCallback<List<ChatMessage>> callback);
}
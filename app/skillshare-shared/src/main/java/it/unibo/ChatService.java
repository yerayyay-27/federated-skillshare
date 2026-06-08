package it.unibo;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("chat")
public interface ChatService extends RemoteService {

    ChatMessage sendMessage(String exchangeRequestId, String senderUsername, String text)
            throws IllegalArgumentException;

    List<ChatMessage> getMessages(String exchangeRequestId, String username)
            throws IllegalArgumentException;
}
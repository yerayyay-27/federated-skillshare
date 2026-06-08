package it.unibo;

import java.util.List;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class ChatServiceImpl extends RemoteServiceServlet implements ChatService {

    private final ChatManager chatManager;

    public ChatServiceImpl() {
        this(new ChatManager());
    }

    ChatServiceImpl(ChatManager chatManager) {
        if (chatManager == null) {
            throw new IllegalArgumentException("Chat manager must not be null");
        }
        this.chatManager = chatManager;
    }

    @Override
    public ChatMessage sendMessage(String exchangeRequestId, String senderUsername, String text)
            throws IllegalArgumentException {
        return chatManager.sendMessage(exchangeRequestId, senderUsername, text);
    }

    @Override
    public List<ChatMessage> getMessages(String exchangeRequestId, String username)
            throws IllegalArgumentException {
        return chatManager.getMessages(exchangeRequestId, username);
    }
}
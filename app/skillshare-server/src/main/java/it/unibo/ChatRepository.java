package it.unibo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.Serializer;

public class ChatRepository {

    // key = exchangeRequestId, value = list of messages of that conversation
    private final ConcurrentMap<String, ArrayList<ChatMessage>> conversations;

    @SuppressWarnings("unchecked")
    public ChatRepository() {
        DB db = DatabaseCore.getDB();
        conversations = (ConcurrentMap<String, ArrayList<ChatMessage>>) (ConcurrentMap<?, ?>) db
                .hashMap("chatConversations", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();
    }

    public void addMessage(String exchangeRequestId, ChatMessage message) {
        ArrayList<ChatMessage> messages = conversations.get(exchangeRequestId);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
        // re-put the modified list so MapDB persists the change
        conversations.put(exchangeRequestId, messages);
        DatabaseCore.commit();
    }

    public List<ChatMessage> getMessages(String exchangeRequestId) {
        ArrayList<ChatMessage> messages = conversations.get(exchangeRequestId);
        return messages == null ? new ArrayList<>() : new ArrayList<>(messages);
    }
}
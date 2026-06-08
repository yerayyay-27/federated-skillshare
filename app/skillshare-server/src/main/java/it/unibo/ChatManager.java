package it.unibo;

import java.util.List;

public class ChatManager {

    private final ChatRepository chatRepository;
    private final ExchangeRequestRepository exchangeRepository;

    public ChatManager() {
        this(new ChatRepository(), new ExchangeRequestRepository());
    }

    public ChatManager(ChatRepository chatRepository, ExchangeRequestRepository exchangeRepository) {
        if (chatRepository == null || exchangeRepository == null) {
            throw new IllegalArgumentException("Repositories must not be null");
        }
        this.chatRepository = chatRepository;
        this.exchangeRepository = exchangeRepository;
    }

    public ChatMessage sendMessage(String exchangeRequestId, String senderUsername, String text) {
        requireNotBlank(exchangeRequestId, "Exchange request id must not be blank");
        requireNotBlank(senderUsername, "Sender username must not be blank");
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Message must not be empty");
        }

        ExchangeRequest request = exchangeRepository.findById(exchangeRequestId);
        ensureAccessibleChat(request, senderUsername);

        ChatMessage message = new ChatMessage(
                exchangeRequestId,
                senderUsername,
                text.trim(),
                System.currentTimeMillis());
        chatRepository.addMessage(exchangeRequestId, message);
        return message;
    }

    public List<ChatMessage> getMessages(String exchangeRequestId, String username) {
        ExchangeRequest request = exchangeRepository.findById(exchangeRequestId);
        ensureAccessibleChat(request, username);
        return chatRepository.getMessages(exchangeRequestId);
    }

    // The chat exists only for accepted requests, and only the two
    // participants (requester and owner) may read or write it.
    private void ensureAccessibleChat(ExchangeRequest request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("Exchange request not found");
        }
        if (!ExchangeRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
            throw new IllegalArgumentException("Chat is available only for accepted requests");
        }
        boolean participant = username != null
                && (username.equals(request.getFromUsername())
                        || username.equals(request.getToUsername()));
        if (!participant) {
            throw new IllegalArgumentException("You are not a participant of this chat");
        }
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
package it.unibo;

import java.util.List;

public class ChatManager {

    private final ChatRepository chatRepository;
    private final ExchangeRequestRepository exchangeRepository;
    private final FederationClient federationClient;

    public ChatManager() {
        this(new ChatRepository(), new ExchangeRequestRepository(), new FederationClient());
    }

    public ChatManager(ChatRepository chatRepository, ExchangeRequestRepository exchangeRepository) {
        this(chatRepository, exchangeRepository, new FederationClient());
    }

    // Full constructor for tests: a federation client can be injected (e.g. a
    // no-op or a capturing one) so unit tests don't perform real network calls.
    ChatManager(
            ChatRepository chatRepository,
            ExchangeRequestRepository exchangeRepository,
            FederationClient federationClient) {
        if (chatRepository == null || exchangeRepository == null) {
            throw new IllegalArgumentException("Repositories must not be null");
        }
        if (federationClient == null) {
            throw new IllegalArgumentException("Federation client must not be null");
        }
        this.chatRepository = chatRepository;
        this.exchangeRepository = exchangeRepository;
        this.federationClient = federationClient;
    }

    public ChatMessage sendMessage(String exchangeRequestId, String senderUsername, String text) {
        requireNotBlank(exchangeRequestId, "Exchange request id must not be blank");
        requireNotBlank(senderUsername, "Sender username must not be blank");
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Message must not be empty");
        }

        ExchangeRequest request = exchangeRepository.findById(exchangeRequestId);
        ensureAccessibleChat(request, senderUsername);

        String localInstance = FederationConfig.get().getInstanceId();
        ChatMessage message = new ChatMessage(
                exchangeRequestId,
                senderUsername,
                text.trim(),
                System.currentTimeMillis());
        message.setSenderInstance(localInstance);

        // Local-first: store on this instance regardless of peer reachability.
        chatRepository.addMessage(exchangeRequestId, message);

        // If the other participant lives on another instance, send the message
        // there so their replica of the conversation converges.
        String recipientInstance = otherInstance(request, localInstance);
        if (recipientInstance != null && !recipientInstance.equals(localInstance)) {
            federationClient.broadcast(
                    FederationEvent.chatMessageCreated(localInstance, message));
        }
        return message;
    }

    public List<ChatMessage> getMessages(String exchangeRequestId, String username) {
        ExchangeRequest request = exchangeRepository.findById(exchangeRequestId);
        ensureAccessibleChat(request, username);
        return chatRepository.getMessages(exchangeRequestId);
    }

    // The chat exists only for accepted requests, and only the two participants
    // (requester and owner) may read or write it. The check is instance-aware:
    // a participant is matched by username AND home instance, so a remote
    // "bob@inst-b" is not treated as the local "bob".
    private void ensureAccessibleChat(ExchangeRequest request, String username) {
        if (request == null) {
            throw new IllegalArgumentException("Exchange request not found");
        }
        if (!ExchangeRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
            throw new IllegalArgumentException("Chat is available only for accepted requests");
        }
        String localInstance = FederationConfig.get().getInstanceId();
        boolean isRequester = username != null
                && username.equals(request.getFromUsername())
                && sameInstance(request.getFromInstance(), localInstance);
        boolean isOwner = username != null
                && username.equals(request.getToUsername())
                && sameInstance(request.getToInstance(), localInstance);
        if (!isRequester && !isOwner) {
            throw new IllegalArgumentException("You are not a participant of this chat");
        }
    }

    // Returns the instance of the OTHER participant, as seen from localInstance.
    // Null instances (pre-federation data) are treated as the local instance.
    private String otherInstance(ExchangeRequest request, String localInstance) {
        String fromInstance = orLocal(request.getFromInstance(), localInstance);
        String toInstance = orLocal(request.getToInstance(), localInstance);
        if (localInstance.equals(fromInstance)) {
            return toInstance;
        }
        if (localInstance.equals(toInstance)) {
            return fromInstance;
        }
        return null;
    }

    private String orLocal(String instance, String localInstance) {
        return instance == null ? localInstance : instance;
    }

    private boolean sameInstance(String instance, String localInstance) {
        return instance == null || instance.equals(localInstance);
    }

    private void requireNotBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }
}
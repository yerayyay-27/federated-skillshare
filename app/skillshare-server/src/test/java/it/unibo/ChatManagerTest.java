package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatManagerTest {

    private static final String LOCAL_INSTANCE = "inst-local"; // FederationConfig default
    private static final String REMOTE_INSTANCE = "inst-b";

    private ChatRepository chatRepository;
    private ExchangeRequestRepository exchangeRepository;
    private CapturingFederationClient federation;
    private ChatManager manager;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        chatRepository = new ChatRepository();
        exchangeRepository = new ExchangeRequestRepository();
        federation = new CapturingFederationClient();
        manager = new ChatManager(chatRepository, exchangeRepository, federation);
        // an accepted exchange between bob (requester) and alice (owner),
        // both on this instance (no instances set -> treated as local)
        exchangeRepository.save(new ExchangeRequest(
                "ex-1", "ann-1", "Java tutoring", "bob", "alice", "hi",
                ExchangeRequest.STATUS_ACCEPTED));
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void sendAndReadMessage() {
        manager.sendMessage("ex-1", "bob", "Hello Alice");

        List<ChatMessage> messages = manager.getMessages("ex-1", "alice");

        assertEquals(1, messages.size());
        assertEquals("bob", messages.get(0).getSenderUsername());
        assertEquals("Hello Alice", messages.get(0).getText());
    }

    @Test
    void bothParticipantsCanWrite() {
        manager.sendMessage("ex-1", "bob", "Hi");
        manager.sendMessage("ex-1", "alice", "Hello");

        assertEquals(2, manager.getMessages("ex-1", "bob").size());
    }

    @Test
    void messagesKeepInsertionOrder() {
        manager.sendMessage("ex-1", "bob", "first");
        manager.sendMessage("ex-1", "alice", "second");

        List<ChatMessage> messages = manager.getMessages("ex-1", "bob");

        assertEquals("first", messages.get(0).getText());
        assertEquals("second", messages.get(1).getText());
    }

    @Test
    void rejectNonParticipant() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.sendMessage("ex-1", "carol", "let me in"));
    }

    @Test
    void rejectBlankMessage() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.sendMessage("ex-1", "bob", "   "));
    }

    @Test
    void rejectUnknownExchange() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.sendMessage("ghost", "bob", "hi"));
    }

    @Test
    void rejectChatForNonAcceptedRequest() {
        exchangeRepository.save(new ExchangeRequest(
                "ex-2", "ann-2", "Piano", "bob", "alice", "hi",
                ExchangeRequest.STATUS_PENDING));

        assertThrows(IllegalArgumentException.class,
                () -> manager.sendMessage("ex-2", "bob", "too early"));
    }

    @Test
    void readingAlsoRejectsNonParticipant() {
        manager.sendMessage("ex-1", "bob", "hi");

        assertThrows(IllegalArgumentException.class,
                () -> manager.getMessages("ex-1", "carol"));
    }

    // --- Federation-specific behaviour ---

    @Test
    void localMessageDoesNotBroadcast() {
        manager.sendMessage("ex-1", "bob", "hello");

        assertTrue(federation.events.isEmpty());
    }

    @Test
    void messageStampsSenderInstance() {
        ChatMessage message = manager.sendMessage("ex-1", "bob", "hello");

        assertEquals(LOCAL_INSTANCE, message.getSenderInstance());
        assertEquals("bob@" + LOCAL_INSTANCE, message.getSenderHandle());
    }

    @Test
    void messageToRemoteParticipantIsBroadcast() {
        // requester is local (bob@inst-local), owner is remote (alice@inst-b)
        ExchangeRequest crossInstance = new ExchangeRequest(
                "ex-remote", "ann-3", "Cooking", "bob", "alice", "hi",
                ExchangeRequest.STATUS_ACCEPTED);
        crossInstance.setFromInstance(LOCAL_INSTANCE);
        crossInstance.setToInstance(REMOTE_INSTANCE);
        exchangeRepository.save(crossInstance);

        manager.sendMessage("ex-remote", "bob", "hello from A");

        assertEquals(1, federation.events.size());
        FederationEvent event = federation.events.get(0);
        assertEquals(FederationEvent.TYPE_CHAT_MESSAGE_CREATED, event.getType());
        assertEquals("hello from A", event.getChatMessage().getText());
    }

    @Test
    void participantCheckIsInstanceAware() {
        // The requester lives on a remote instance (bob@inst-b); the owner is
        // local (alice@inst-local). The local "bob" must NOT pass as a
        // participant, but the local "alice" must.
        ExchangeRequest crossInstance = new ExchangeRequest(
                "ex-owner", "ann-4", "Guitar", "bob", "alice", "hi",
                ExchangeRequest.STATUS_ACCEPTED);
        crossInstance.setFromInstance(REMOTE_INSTANCE);
        crossInstance.setToInstance(LOCAL_INSTANCE);
        exchangeRepository.save(crossInstance);

        assertThrows(IllegalArgumentException.class,
                () -> manager.sendMessage("ex-owner", "bob", "I am the remote bob"));

        ChatMessage ownerMessage = manager.sendMessage("ex-owner", "alice", "I am the local alice");
        assertEquals("alice", ownerMessage.getSenderUsername());
        // owner is local, requester is remote -> message is federated back
        assertEquals(1, federation.events.size());
        assertEquals(FederationEvent.TYPE_CHAT_MESSAGE_CREATED,
                federation.events.get(0).getType());
    }

    // --- Logical-clock ordering ---

    @Test
    void messagesGetIncreasingLamportTimestamps() {
        ChatMessage first = manager.sendMessage("ex-1", "bob", "1");
        ChatMessage second = manager.sendMessage("ex-1", "alice", "2");

        assertTrue(first.getLamportTimestamp() < second.getLamportTimestamp());
    }

    @Test
    void getMessagesOrdersByLamportNotInsertion() {
        // Simulate a remote message that arrived out of order: it was sent
        // earlier (lower Lamport) but stored after a local message.
        ChatMessage local = manager.sendMessage("ex-1", "alice", "local later text");
        long localTs = local.getLamportTimestamp();

        ChatMessage earlierRemote = new ChatMessage("ex-1", "bob", "earlier remote text",
                System.currentTimeMillis());
        earlierRemote.setSenderInstance(REMOTE_INSTANCE);
        earlierRemote.setLamportTimestamp(localTs - 1);
        chatRepository.addMessage("ex-1", earlierRemote); // appended last

        List<ChatMessage> messages = manager.getMessages("ex-1", "alice");

        assertEquals("earlier remote text", messages.get(0).getText());
        assertEquals("local later text", messages.get(1).getText());
    }

    @Test
    void concurrentMessagesBreakTiesByInstance() {
        // Two messages with the SAME Lamport timestamp (concurrent). The tie is
        // broken deterministically by instance id, so every replica agrees.
        ChatMessage fromB = new ChatMessage("ex-1", "bob", "from B",
                System.currentTimeMillis());
        fromB.setSenderInstance(REMOTE_INSTANCE); // "inst-b"
        fromB.setLamportTimestamp(7);

        ChatMessage fromLocal = new ChatMessage("ex-1", "alice", "from local",
                System.currentTimeMillis());
        fromLocal.setSenderInstance(LOCAL_INSTANCE); // "inst-local"
        fromLocal.setLamportTimestamp(7);

        chatRepository.addMessage("ex-1", fromB);     // appended first
        chatRepository.addMessage("ex-1", fromLocal); // appended second

        List<ChatMessage> messages = manager.getMessages("ex-1", "alice");

        // "inst-b" sorts before "inst-local"
        assertEquals("from B", messages.get(0).getText());
        assertEquals("from local", messages.get(1).getText());
    }

    private static class CapturingFederationClient extends FederationClient {
        final List<FederationEvent> events = new ArrayList<>();

        @Override
        public void broadcast(FederationEvent event) {
            events.add(event);
        }
    }
}
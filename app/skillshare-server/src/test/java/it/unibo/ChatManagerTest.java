package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatManagerTest {

    private ExchangeRequestRepository exchangeRepository;
    private ChatManager manager;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        exchangeRepository = new ExchangeRequestRepository();
        manager = new ChatManager(new ChatRepository(), exchangeRepository);
        // an accepted exchange between bob (requester) and alice (owner)
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
}
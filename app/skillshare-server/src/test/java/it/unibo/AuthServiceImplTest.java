package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceImplTest {

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        service = new AuthServiceImpl(new UserRepository());
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void registerCreatesUser() {
        User user = service.register("Alice", "alice@unibo.it", "secret");

        assertNotNull(user);
        assertEquals("Alice", user.getUsername());
        assertEquals("alice@unibo.it", user.getEmail());
    }

    @Test
    void registerThenLoginSucceeds() {
        service.register("Alice", "alice@unibo.it", "secret");

        User user = service.login("alice@unibo.it", "secret");

        assertNotNull(user);
        assertEquals("Alice", user.getUsername());
    }

    @Test
    void rejectDuplicateEmail() {
        service.register("Alice", "alice@unibo.it", "secret");

        assertThrows(IllegalArgumentException.class,
                () -> service.register("Another", "alice@unibo.it", "another"));
    }

    @Test
    void rejectShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("Bob", "bob@unibo.it", "123"));
    }

    @Test
    void rejectBlankRegistrationFields() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register(" ", "bob@unibo.it", "secret"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("Bob", " ", "secret"));
    }

    @Test
    void loginFailsWithWrongPassword() {
        service.register("Alice", "alice@unibo.it", "secret");

        assertThrows(IllegalArgumentException.class,
                () -> service.login("alice@unibo.it", "wrong"));
    }

    @Test
    void loginFailsForUnknownUser() {
        assertThrows(IllegalArgumentException.class,
                () -> service.login("ghost@unibo.it", "secret"));
    }

    @Test
    void rejectBlankLoginFields() {
        assertThrows(IllegalArgumentException.class,
                () -> service.login("", "secret"));
        assertThrows(IllegalArgumentException.class,
                () -> service.login("alice@unibo.it", ""));
    }

    @Test
    void seededTestUserCanLogIn() {
        User user = service.login("test@unibo.it", "1234");

        assertNotNull(user);
        assertEquals("TestUser", user.getUsername());
    }
}
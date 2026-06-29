package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        LoginResult result = service.login("alice@unibo.it", "secret");

        assertTrue(result.isSuccess());
        assertNotNull(result.getUser());
        assertEquals("Alice", result.getUser().getUsername());
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

        LoginResult result = service.login("alice@unibo.it", "wrong");

        assertFalse(result.isSuccess());
        assertEquals("Incorrect password.", result.getErrorReason());
    }

    @Test
    void loginFailsForUnknownUser() {
        LoginResult result = service.login("ghost@unibo.it", "secret");

        assertFalse(result.isSuccess());
        assertEquals("No account found for this email.", result.getErrorReason());
    }

    @Test
    void rejectBlankLoginFields() {
        LoginResult blankEmail = service.login("", "secret");
        LoginResult blankPassword = service.login("alice@unibo.it", "");

        assertFalse(blankEmail.isSuccess());
        assertNotNull(blankEmail.getErrorReason());
        assertFalse(blankPassword.isSuccess());
        assertNotNull(blankPassword.getErrorReason());
    }

    @Test
    void seededTestUserCanLogIn() {
        LoginResult result = service.login("test@unibo.it", "1234");

        assertTrue(result.isSuccess());
        assertEquals("TestUser", result.getUser().getUsername());
    }
}
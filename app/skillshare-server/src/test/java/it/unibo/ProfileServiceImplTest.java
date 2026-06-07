package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileServiceImplTest {

    private UserRepository userRepository;
    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        userRepository = new UserRepository();
        service = new ProfileServiceImpl(userRepository);
        userRepository.create("alice@unibo.it", "Alice", "secret");
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void updateProfileStoresBioAndTags() {
        List<String> tags = Arrays.asList("Java", "Guitar");

        User updated = service.updateProfile("alice@unibo.it", "Hello world", tags);

        assertNotNull(updated);
        assertEquals("Hello world", updated.getBio());
        assertEquals(tags, updated.getSkillTags());
    }

    @Test
    void updatedProfilePersistsAcrossReads() {
        service.updateProfile("alice@unibo.it", "Bio text", Arrays.asList("Cooking"));

        User reloaded = userRepository.getUser("alice@unibo.it");

        assertEquals("Bio text", reloaded.getBio());
        assertEquals(Arrays.asList("Cooking"), reloaded.getSkillTags());
    }

    @Test
    void rejectUnknownUser() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile("ghost@unibo.it", "x", Arrays.asList("y")));
    }

    @Test
    void rejectBlankEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateProfile(" ", "x", Arrays.asList("y")));
    }

    @Test
    void handleNullBioAndTags() {
        User updated = service.updateProfile("alice@unibo.it", null, null);

        assertEquals("", updated.getBio());
        assertTrue(updated.getSkillTags().isEmpty());
    }
}
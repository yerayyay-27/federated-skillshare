package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnnouncementManagerTest {

    private AnnouncementManager manager;

    // No-op federation client: unit tests must not perform real network calls.
    private static final FederationClient NO_FEDERATION = new FederationClient() {
        @Override
        public void broadcast(FederationEvent event) {
            // intentionally does nothing in tests
        }
    };

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        manager = new AnnouncementManager(
                new AnnouncementRepository(),
                () -> java.util.UUID.randomUUID().toString(),
                NO_FEDERATION);
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void createValidAnnouncement() {
        Announcement announcement = validAnnouncement(null);

        Announcement createdAnnouncement = manager.createAnnouncement(announcement);

        assertEquals(announcement, createdAnnouncement);
        assertNotNull(createdAnnouncement.getId());
        assertNotNull(manager.getAnnouncementById(createdAnnouncement.getId()));
    }

    @Test
    void replaceManuallySuppliedIdDuringCreation() {
        Announcement announcement = validAnnouncement("client-supplied-id");

        Announcement createdAnnouncement = manager.createAnnouncement(announcement);

        assertFalse("client-supplied-id".equals(createdAnnouncement.getId()));
        assertNotNull(manager.getAnnouncementById(createdAnnouncement.getId()));
    }

    @Test
    void rejectBlankOwnerUsername() {
        Announcement announcement = validAnnouncement(null);
        announcement.setOwnerUsername(" ");

        assertThrows(IllegalArgumentException.class, () -> manager.createAnnouncement(announcement));
    }

    @Test
    void rejectBlankOfferedSkill() {
        Announcement announcement = validAnnouncement(null);
        announcement.setOfferedSkill(null);

        assertThrows(IllegalArgumentException.class, () -> manager.createAnnouncement(announcement));
    }

    @Test
    void rejectBlankRequestedSkill() {
        Announcement announcement = validAnnouncement(null);
        announcement.setRequestedSkill("");

        assertThrows(IllegalArgumentException.class, () -> manager.createAnnouncement(announcement));
    }

    @Test
    void retryWhenGeneratedIdAlreadyExists() {
        AnnouncementRepository repository = new AnnouncementRepository();
        repository.save(validAnnouncement("collision-id"));
        AtomicInteger attempts = new AtomicInteger();
        AnnouncementManager collisionManager = new AnnouncementManager(
                repository,
                () -> attempts.getAndIncrement() == 0
                        ? "collision-id"
                        : "generated-id",
                NO_FEDERATION);

        Announcement createdAnnouncement =
                collisionManager.createAnnouncement(validAnnouncement(null));

        assertEquals("generated-id", createdAnnouncement.getId());
        assertNotNull(collisionManager.getAnnouncementById("generated-id"));
    }

    @Test
    void retrieveAnnouncementById() {
        Announcement created = manager.createAnnouncement(validAnnouncement(null));

        Announcement announcement = manager.getAnnouncementById(created.getId());

        assertNotNull(announcement);
        assertEquals("alice", announcement.getOwnerUsername());
    }

    @Test
    void listOnlyActiveAnnouncements() {
        Announcement activeAnnouncement = manager.createAnnouncement(validAnnouncement(null));
        Announcement inactiveAnnouncement = manager.createAnnouncement(validAnnouncement(null));
        manager.deactivateAnnouncement(inactiveAnnouncement.getId());

        List<Announcement> announcements = manager.getActiveAnnouncements();

        assertEquals(1, announcements.size());
        assertEquals(activeAnnouncement.getId(), announcements.get(0).getId());
    }

    @Test
    void deactivateExistingAnnouncement() {
        Announcement created = manager.createAnnouncement(validAnnouncement(null));

        boolean deactivated = manager.deactivateAnnouncement(created.getId());

        assertTrue(deactivated);
        assertFalse(manager.getAnnouncementById(created.getId()).isActive());
    }

    @Test
    void searchAnnouncementsCaseInsensitively() {
        Announcement javaAnnouncement = validAnnouncement(null);
        Announcement pianoAnnouncement = new Announcement(
                null,
                "bob",
                "Piano lessons",
                "Photography",
                "Learn classical music.",
                "Saturday mornings",
                true);
        Announcement inactiveAnnouncement = new Announcement(
                null,
                "carol",
                "Advanced Java",
                "Cooking",
                "Backend mentoring.",
                "Sundays",
                false);
        manager.createAnnouncement(javaAnnouncement);
        manager.createAnnouncement(pianoAnnouncement);
        manager.createAnnouncement(inactiveAnnouncement);
        manager.deactivateAnnouncement(inactiveAnnouncement.getId());

        List<Announcement> offeredSkillMatches = manager.searchActiveAnnouncements("jAvA");
        List<Announcement> requestedSkillMatches = manager.searchActiveAnnouncements("PHOTO");
        List<Announcement> descriptionMatches = manager.searchActiveAnnouncements("classical");

        assertEquals(1, offeredSkillMatches.size());
        assertEquals(javaAnnouncement.getId(), offeredSkillMatches.get(0).getId());
        assertEquals(1, requestedSkillMatches.size());
        assertEquals(pianoAnnouncement.getId(), requestedSkillMatches.get(0).getId());
        assertEquals(1, descriptionMatches.size());
        assertEquals(pianoAnnouncement.getId(), descriptionMatches.get(0).getId());
    }

    @Test
    void updateOwnedAnnouncementAndPreserveProtectedFields() {
        Announcement created = manager.createAnnouncement(validAnnouncement(null));
        manager.deactivateAnnouncement(created.getId());
        Announcement update = new Announcement(
                created.getId(),
                "mallory",
                "Advanced Java",
                "French conversation",
                "Updated description.",
                "Friday evenings",
                true);

        Announcement updated = manager.updateAnnouncement("alice", update);

        assertEquals(created.getId(), updated.getId());
        assertEquals("alice", updated.getOwnerUsername());
        assertFalse(updated.isActive());
        assertEquals("Advanced Java", updated.getOfferedSkill());
    }

    @Test
    void rejectUpdateFromAnotherUsername() {
        Announcement created = manager.createAnnouncement(validAnnouncement(null));

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.updateAnnouncement("bob", created));
    }

    @Test
    void rejectNullAnnouncementUpdate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.updateAnnouncement("alice", null));
    }

    @Test
    void rejectBlankAnnouncementIdDuringUpdate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.updateAnnouncement(
                        "alice",
                        validAnnouncement(" ")));
    }

    @Test
    void rejectBlankOwnerUsernameDuringUpdate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.updateAnnouncement(
                        " ",
                        validAnnouncement("announcement-1")));
    }

    @Test
    void rejectBlankOfferedSkillDuringUpdate() {
        Announcement update = validAnnouncement("announcement-1");
        update.setOfferedSkill(" ");

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.updateAnnouncement("alice", update));
    }

    @Test
    void rejectBlankRequestedSkillDuringUpdate() {
        Announcement update = validAnnouncement("announcement-1");
        update.setRequestedSkill(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.updateAnnouncement("alice", update));
    }

    @Test
    void rejectUnknownAnnouncementUpdate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.updateAnnouncement(
                        "alice",
                        validAnnouncement("unknown-id")));
    }

    @Test
    void deleteOwnedAnnouncement() {
        Announcement created = manager.createAnnouncement(validAnnouncement(null));

        assertTrue(manager.deleteAnnouncement(created.getId(), "alice"));
        assertEquals(null, manager.getAnnouncementById(created.getId()));
    }

    @Test
    void rejectDeletionFromAnotherUsername() {
        Announcement created = manager.createAnnouncement(validAnnouncement(null));

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.deleteAnnouncement(created.getId(), "bob"));
    }

    @Test
    void rejectBlankAnnouncementIdDuringDeletion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.deleteAnnouncement(" ", "alice"));
    }

    @Test
    void rejectBlankOwnerUsernameDuringDeletion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.deleteAnnouncement("announcement-1", " "));
    }

    @Test
    void rejectUnknownAnnouncementDeletion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> manager.deleteAnnouncement("unknown-id", "alice"));
    }

    private Announcement validAnnouncement(String id) {
        return new Announcement(
                id,
                "alice",
                "Java tutoring",
                "Spanish conversation",
                "I teach practical Java fundamentals.",
                "Weekday evenings",
                true);
    }
}
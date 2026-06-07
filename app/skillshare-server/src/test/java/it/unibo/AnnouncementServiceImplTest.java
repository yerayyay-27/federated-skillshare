package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnnouncementServiceImplTest {

    private AnnouncementServiceImpl service;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        AnnouncementRepository repository = new AnnouncementRepository();
        service = new AnnouncementServiceImpl(new AnnouncementManager(repository));
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void createValidAnnouncement() {
        Announcement createdAnnouncement = service.createAnnouncement(
                announcement(null, "alice", "Java tutoring", true));

        assertNotNull(createdAnnouncement);
        assertNotNull(createdAnnouncement.getId());
        assertNotNull(service.getAnnouncementById(createdAnnouncement.getId()));
    }

    @Test
    void replaceClientSuppliedId() {
        Announcement createdAnnouncement = service.createAnnouncement(
                announcement("client-id", "alice", "Java tutoring", true));

        assertFalse("client-id".equals(createdAnnouncement.getId()));
    }

    @Test
    void retrieveExistingAnnouncement() {
        Announcement created = service.createAnnouncement(
                announcement(null, "alice", "Java tutoring", true));

        Announcement storedAnnouncement = service.getAnnouncementById(created.getId());

        assertNotNull(storedAnnouncement);
        assertEquals("Java tutoring", storedAnnouncement.getOfferedSkill());
    }

    @Test
    void listOnlyActiveAnnouncements() {
        Announcement active = service.createAnnouncement(
                announcement(null, "alice", "Java tutoring", true));
        Announcement inactive = service.createAnnouncement(
                announcement(null, "bob", "Piano lessons", true));
        service.deactivateAnnouncement(inactive.getId());

        List<Announcement> announcements = service.getActiveAnnouncements();

        assertEquals(1, announcements.size());
        assertEquals(active.getId(), announcements.get(0).getId());
    }

    @Test
    void deactivateExistingAnnouncement() {
        Announcement created = service.createAnnouncement(
                announcement(null, "alice", "Java tutoring", true));

        boolean deactivated = service.deactivateAnnouncement(created.getId());

        assertTrue(deactivated);
        assertFalse(service.getAnnouncementById(created.getId()).isActive());
    }

    @Test
    void searchAnnouncementsCaseInsensitively() {
        Announcement javaAnnouncement = service.createAnnouncement(
                announcement(null, "alice", "Java tutoring", true));
        Announcement pianoAnnouncement = service.createAnnouncement(new Announcement(
                null,
                "bob",
                "Piano lessons",
                "Photography",
                "Learn classical music.",
                "Saturday mornings",
                true));
        Announcement inactiveAnnouncement = service.createAnnouncement(
                announcement(null, "carol", "Advanced Java", true));
        service.deactivateAnnouncement(inactiveAnnouncement.getId());

        List<Announcement> offeredSkillMatches = service.searchActiveAnnouncements("jAvA");
        List<Announcement> requestedSkillMatches = service.searchActiveAnnouncements("PHOTO");
        List<Announcement> descriptionMatches = service.searchActiveAnnouncements("classical");

        assertEquals(1, offeredSkillMatches.size());
        assertEquals(javaAnnouncement.getId(), offeredSkillMatches.get(0).getId());
        assertEquals(1, requestedSkillMatches.size());
        assertEquals(pianoAnnouncement.getId(), requestedSkillMatches.get(0).getId());
        assertEquals(1, descriptionMatches.size());
        assertEquals(pianoAnnouncement.getId(), descriptionMatches.get(0).getId());
    }

    @Test
    void delegateValidationErrors() {
        Announcement invalidAnnouncement =
                announcement(null, " ", "Java tutoring", true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createAnnouncement(invalidAnnouncement));

        assertEquals("Owner username must not be blank", exception.getMessage());
    }

    @Test
    void updateAnnouncement() {
        Announcement created = service.createAnnouncement(
                announcement(null, "alice", "Java tutoring", true));
        created.setOfferedSkill("Advanced Java");

        Announcement updated = service.updateAnnouncement("alice", created);

        assertEquals("Advanced Java", updated.getOfferedSkill());
        assertEquals(created.getId(), updated.getId());
    }

    @Test
    void propagateInvalidUpdateError() {
        Announcement created = service.createAnnouncement(
                announcement(null, "alice", "Java tutoring", true));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateAnnouncement("bob", created));
    }

    @Test
    void deleteAnnouncement() {
        Announcement created = service.createAnnouncement(
                announcement(null, "alice", "Java tutoring", true));

        assertTrue(service.deleteAnnouncement(created.getId(), "alice"));
        assertEquals(null, service.getAnnouncementById(created.getId()));
    }

    @Test
    void propagateUnauthorizedDeletionError() {
        Announcement created = service.createAnnouncement(
                announcement(null, "alice", "Java tutoring", true));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteAnnouncement(created.getId(), "bob"));
    }

    private Announcement announcement(
            String id,
            String ownerUsername,
            String offeredSkill,
            boolean active) {
        return new Announcement(
                id,
                ownerUsername,
                offeredSkill,
                "Spanish conversation",
                "I teach practical skills.",
                "Weekday evenings",
                active);
    }
}

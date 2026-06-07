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
                announcement("announcement-1", "alice", "Java tutoring", true));

        assertNotNull(createdAnnouncement);
        assertEquals("announcement-1", createdAnnouncement.getId());
        assertNotNull(service.getAnnouncementById("announcement-1"));
    }

    @Test
    void rejectDuplicatedAnnouncementId() {
        service.createAnnouncement(
                announcement("announcement-1", "alice", "Java tutoring", true));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createAnnouncement(
                        announcement("announcement-1", "bob", "Piano lessons", true)));
        assertEquals("alice", service.getAnnouncementById("announcement-1").getOwnerUsername());
    }

    @Test
    void retrieveExistingAnnouncement() {
        service.createAnnouncement(
                announcement("announcement-1", "alice", "Java tutoring", true));

        Announcement storedAnnouncement = service.getAnnouncementById("announcement-1");

        assertNotNull(storedAnnouncement);
        assertEquals("Java tutoring", storedAnnouncement.getOfferedSkill());
    }

    @Test
    void listOnlyActiveAnnouncements() {
        service.createAnnouncement(
                announcement("announcement-1", "alice", "Java tutoring", true));
        service.createAnnouncement(
                announcement("announcement-2", "bob", "Piano lessons", false));

        List<Announcement> announcements = service.getActiveAnnouncements();

        assertEquals(1, announcements.size());
        assertEquals("announcement-1", announcements.get(0).getId());
    }

    @Test
    void deactivateExistingAnnouncement() {
        service.createAnnouncement(
                announcement("announcement-1", "alice", "Java tutoring", true));

        boolean deactivated = service.deactivateAnnouncement("announcement-1");

        assertTrue(deactivated);
        assertFalse(service.getAnnouncementById("announcement-1").isActive());
    }

    @Test
    void searchAnnouncementsCaseInsensitively() {
        service.createAnnouncement(
                announcement("announcement-1", "alice", "Java tutoring", true));
        service.createAnnouncement(new Announcement(
                "announcement-2",
                "bob",
                "Piano lessons",
                "Photography",
                "Learn classical music.",
                "Saturday mornings",
                true));
        service.createAnnouncement(
                announcement("announcement-3", "carol", "Advanced Java", false));

        List<Announcement> offeredSkillMatches = service.searchActiveAnnouncements("jAvA");
        List<Announcement> requestedSkillMatches = service.searchActiveAnnouncements("PHOTO");
        List<Announcement> descriptionMatches = service.searchActiveAnnouncements("classical");

        assertEquals(1, offeredSkillMatches.size());
        assertEquals("announcement-1", offeredSkillMatches.get(0).getId());
        assertEquals(1, requestedSkillMatches.size());
        assertEquals("announcement-2", requestedSkillMatches.get(0).getId());
        assertEquals(1, descriptionMatches.size());
        assertEquals("announcement-2", descriptionMatches.get(0).getId());
    }

    @Test
    void delegateValidationErrors() {
        Announcement invalidAnnouncement =
                announcement("announcement-1", " ", "Java tutoring", true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createAnnouncement(invalidAnnouncement));

        assertEquals("Owner username must not be blank", exception.getMessage());
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

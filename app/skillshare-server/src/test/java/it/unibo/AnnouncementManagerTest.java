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

class AnnouncementManagerTest {

    private AnnouncementManager manager;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        manager = new AnnouncementManager(new AnnouncementRepository());
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void createValidAnnouncement() {
        Announcement announcement = validAnnouncement("announcement-1");

        Announcement createdAnnouncement = manager.createAnnouncement(announcement);

        assertEquals(announcement, createdAnnouncement);
        assertNotNull(manager.getAnnouncementById("announcement-1"));
    }

    @Test
    void rejectBlankId() {
        Announcement announcement = validAnnouncement(" ");

        assertThrows(IllegalArgumentException.class, () -> manager.createAnnouncement(announcement));
    }

    @Test
    void rejectBlankOwnerUsername() {
        Announcement announcement = validAnnouncement("announcement-1");
        announcement.setOwnerUsername(" ");

        assertThrows(IllegalArgumentException.class, () -> manager.createAnnouncement(announcement));
    }

    @Test
    void rejectBlankOfferedSkill() {
        Announcement announcement = validAnnouncement("announcement-1");
        announcement.setOfferedSkill(null);

        assertThrows(IllegalArgumentException.class, () -> manager.createAnnouncement(announcement));
    }

    @Test
    void rejectBlankRequestedSkill() {
        Announcement announcement = validAnnouncement("announcement-1");
        announcement.setRequestedSkill("");

        assertThrows(IllegalArgumentException.class, () -> manager.createAnnouncement(announcement));
    }

    @Test
    void rejectDuplicatedId() {
        Announcement originalAnnouncement = validAnnouncement("announcement-1");
        Announcement duplicatedAnnouncement = validAnnouncement("announcement-1");
        duplicatedAnnouncement.setOwnerUsername("bob");
        duplicatedAnnouncement.setOfferedSkill("Piano lessons");
        manager.createAnnouncement(originalAnnouncement);

        assertThrows(
                IllegalArgumentException.class,
                () -> manager.createAnnouncement(duplicatedAnnouncement));
        assertEquals("alice", manager.getAnnouncementById("announcement-1").getOwnerUsername());
        assertEquals("Java tutoring", manager.getAnnouncementById("announcement-1").getOfferedSkill());
    }

    @Test
    void retrieveAnnouncementById() {
        manager.createAnnouncement(validAnnouncement("announcement-1"));

        Announcement announcement = manager.getAnnouncementById("announcement-1");

        assertNotNull(announcement);
        assertEquals("alice", announcement.getOwnerUsername());
    }

    @Test
    void listOnlyActiveAnnouncements() {
        Announcement activeAnnouncement = validAnnouncement("announcement-1");
        Announcement inactiveAnnouncement = validAnnouncement("announcement-2");
        inactiveAnnouncement.setActive(false);
        manager.createAnnouncement(activeAnnouncement);
        manager.createAnnouncement(inactiveAnnouncement);

        List<Announcement> announcements = manager.getActiveAnnouncements();

        assertEquals(1, announcements.size());
        assertEquals("announcement-1", announcements.get(0).getId());
    }

    @Test
    void deactivateExistingAnnouncement() {
        manager.createAnnouncement(validAnnouncement("announcement-1"));

        boolean deactivated = manager.deactivateAnnouncement("announcement-1");

        assertTrue(deactivated);
        assertFalse(manager.getAnnouncementById("announcement-1").isActive());
    }

    @Test
    void searchAnnouncementsCaseInsensitively() {
        Announcement javaAnnouncement = validAnnouncement("announcement-1");
        Announcement pianoAnnouncement = new Announcement(
                "announcement-2",
                "bob",
                "Piano lessons",
                "Photography",
                "Learn classical music.",
                "Saturday mornings",
                true);
        Announcement inactiveAnnouncement = new Announcement(
                "announcement-3",
                "carol",
                "Advanced Java",
                "Cooking",
                "Backend mentoring.",
                "Sundays",
                false);
        manager.createAnnouncement(javaAnnouncement);
        manager.createAnnouncement(pianoAnnouncement);
        manager.createAnnouncement(inactiveAnnouncement);

        List<Announcement> offeredSkillMatches = manager.searchActiveAnnouncements("jAvA");
        List<Announcement> requestedSkillMatches = manager.searchActiveAnnouncements("PHOTO");
        List<Announcement> descriptionMatches = manager.searchActiveAnnouncements("classical");

        assertEquals(1, offeredSkillMatches.size());
        assertEquals("announcement-1", offeredSkillMatches.get(0).getId());
        assertEquals(1, requestedSkillMatches.size());
        assertEquals("announcement-2", requestedSkillMatches.get(0).getId());
        assertEquals(1, descriptionMatches.size());
        assertEquals("announcement-2", descriptionMatches.get(0).getId());
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

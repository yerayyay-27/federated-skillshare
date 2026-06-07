package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnnouncementRepositoryTest {

    private AnnouncementRepository repository;

    @BeforeEach
    void setUp() {
        DatabaseCore.enableTestMode();
        repository = new AnnouncementRepository();
    }

    @AfterEach
    void tearDown() {
        DatabaseCore.disableTestMode();
    }

    @Test
    void saveAndFindAnnouncementById() {
        Announcement announcement = announcement("announcement-1", true);

        boolean inserted = repository.save(announcement);

        assertTrue(inserted);
        Announcement storedAnnouncement = repository.findById("announcement-1");
        assertNotNull(storedAnnouncement);
        assertEquals("Java tutoring", storedAnnouncement.getOfferedSkill());
    }

    @Test
    void rejectDuplicatedId() {
        repository.save(announcement("announcement-1", true));

        boolean inserted = repository.save(announcement("announcement-1", false));

        assertFalse(inserted);
    }

    @Test
    void rejectDuplicatedIdAcrossRepositoryInstances() {
        AnnouncementRepository secondRepository = new AnnouncementRepository();
        repository.save(announcement("announcement-1", true));

        boolean inserted = secondRepository.save(announcement("announcement-1", false));

        assertFalse(inserted);
    }

    @Test
    void duplicatedInsertionDoesNotOverwriteOriginalAnnouncement() {
        Announcement originalAnnouncement = announcement("announcement-1", true);
        Announcement duplicatedAnnouncement = new Announcement(
                "announcement-1",
                "bob",
                "Piano lessons",
                "Photography",
                "Learn classical music.",
                "Saturday mornings",
                false);
        AnnouncementRepository secondRepository = new AnnouncementRepository();
        repository.save(originalAnnouncement);

        secondRepository.save(duplicatedAnnouncement);

        Announcement storedAnnouncement = repository.findById("announcement-1");
        assertEquals("alice", storedAnnouncement.getOwnerUsername());
        assertEquals("Java tutoring", storedAnnouncement.getOfferedSkill());
        assertTrue(storedAnnouncement.isActive());
    }

    @Test
    void listAllAnnouncements() {
        repository.save(announcement("announcement-1", true));
        repository.save(announcement("announcement-2", false));

        List<Announcement> announcements = repository.listAll();

        assertEquals(2, announcements.size());
    }

    @Test
    void deactivateAnnouncement() {
        repository.save(announcement("announcement-1", true));

        boolean deactivated = repository.deactivateById("announcement-1");

        assertTrue(deactivated);
        assertFalse(repository.findById("announcement-1").isActive());
    }

    @Test
    void rejectOrHandleUnknownAnnouncementWhenDeactivating() {
        boolean deactivated = repository.deactivateById("unknown-id");

        assertFalse(deactivated);
    }

    @Test
    void updateExistingAnnouncement() {
        repository.save(announcement("announcement-1", true));
        Announcement updatedAnnouncement = new Announcement(
                "announcement-1",
                "alice",
                "Advanced Java",
                "French conversation",
                "Updated description.",
                "Friday evenings",
                true);

        boolean updated = repository.update(updatedAnnouncement);

        assertTrue(updated);
        assertEquals(
                "Advanced Java",
                repository.findById("announcement-1").getOfferedSkill());
    }

    @Test
    void rejectUnknownAnnouncementWhenUpdating() {
        boolean updated = repository.update(announcement("unknown-id", true));

        assertFalse(updated);
    }

    @Test
    void deleteExistingAnnouncement() {
        repository.save(announcement("announcement-1", true));

        boolean deleted = repository.deleteById("announcement-1");

        assertTrue(deleted);
        assertEquals(null, repository.findById("announcement-1"));
    }

    @Test
    void rejectUnknownAnnouncementWhenDeleting() {
        boolean deleted = repository.deleteById("unknown-id");

        assertFalse(deleted);
    }

    private Announcement announcement(String id, boolean active) {
        return new Announcement(
                id,
                "alice",
                "Java tutoring",
                "Spanish conversation",
                "I teach practical Java fundamentals.",
                "Weekday evenings",
                active);
    }
}

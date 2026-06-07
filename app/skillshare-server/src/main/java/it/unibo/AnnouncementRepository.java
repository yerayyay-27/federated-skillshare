package it.unibo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.Serializer;

public class AnnouncementRepository {

    private final ConcurrentMap<String, Announcement> announcements;

    @SuppressWarnings("unchecked")
    public AnnouncementRepository() {
        DB db = DatabaseCore.getDB();
        announcements = (ConcurrentMap<String, Announcement>) (ConcurrentMap<?, ?>) db
                .hashMap("announcements", Serializer.STRING, Serializer.JAVA)
                .createOrOpen();
    }

    public boolean save(Announcement announcement) {
        Announcement existingAnnouncement = announcements.putIfAbsent(announcement.getId(), announcement);
        if (existingAnnouncement != null) {
            return false;
        }

        DatabaseCore.commit();
        return true;
    }

    public Announcement findById(String id) {
        return announcements.get(id);
    }

    public List<Announcement> listAll() {
        return new ArrayList<>(announcements.values());
    }

    public boolean deactivateById(String id) {
        Announcement announcement = announcements.get(id);
        if (announcement == null) {
            return false;
        }

        announcement.setActive(false);
        announcements.put(id, announcement);
        DatabaseCore.commit();
        return true;
    }

    public boolean update(Announcement announcement) {
        boolean updated = announcements.replace(announcement.getId(), announcement) != null;
        if (updated) {
            DatabaseCore.commit();
        }
        return updated;
    }

    public boolean deleteById(String id) {
        Announcement removedAnnouncement = announcements.remove(id);
        if (removedAnnouncement == null) {
            return false;
        }

        DatabaseCore.commit();
        return true;
    }
}

package it.unibo;

import java.util.List;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class AnnouncementServiceImpl extends RemoteServiceServlet implements AnnouncementService {

    private final AnnouncementManager announcementManager;

    public AnnouncementServiceImpl() {
        this(new AnnouncementManager());
    }

    AnnouncementServiceImpl(AnnouncementManager announcementManager) {
        if (announcementManager == null) {
            throw new IllegalArgumentException("Announcement manager must not be null");
        }
        this.announcementManager = announcementManager;
    }

    @Override
    public Announcement createAnnouncement(Announcement announcement) throws IllegalArgumentException {
        return announcementManager.createAnnouncement(announcement);
    }

    @Override
    public Announcement getAnnouncementById(String id) {
        return announcementManager.getAnnouncementById(id);
    }

    @Override
    public List<Announcement> getActiveAnnouncements() {
        return announcementManager.getActiveAnnouncements();
    }

    @Override
    public boolean deactivateAnnouncement(String id) {
        return announcementManager.deactivateAnnouncement(id);
    }

    @Override
    public List<Announcement> searchActiveAnnouncements(String query) {
        return announcementManager.searchActiveAnnouncements(query);
    }

    @Override
    public Announcement updateAnnouncement(
            String ownerUsername,
            Announcement announcement) throws IllegalArgumentException {
        return announcementManager.updateAnnouncement(ownerUsername, announcement);
    }

    @Override
    public boolean deleteAnnouncement(
            String id,
            String ownerUsername) throws IllegalArgumentException {
        return announcementManager.deleteAnnouncement(id, ownerUsername);
    }
}

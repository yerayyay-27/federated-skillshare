package it.unibo;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("announcements")
public interface AnnouncementService extends RemoteService {

    Announcement createAnnouncement(Announcement announcement) throws IllegalArgumentException;

    Announcement getAnnouncementById(String id);

    List<Announcement> getActiveAnnouncements();

    boolean deactivateAnnouncement(String id);

    List<Announcement> searchActiveAnnouncements(String query);

    Announcement updateAnnouncement(String ownerUsername, Announcement announcement)
            throws IllegalArgumentException;

    boolean deleteAnnouncement(String id, String ownerUsername)
            throws IllegalArgumentException;
}

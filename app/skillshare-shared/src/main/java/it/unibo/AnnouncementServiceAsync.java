package it.unibo;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface AnnouncementServiceAsync {

    void createAnnouncement(
            Announcement announcement,
            AsyncCallback<Announcement> callback);

    void getAnnouncementById(
            String id,
            AsyncCallback<Announcement> callback);

    void getActiveAnnouncements(
            AsyncCallback<List<Announcement>> callback);

    void deactivateAnnouncement(
            String id,
            AsyncCallback<Boolean> callback);

    void searchActiveAnnouncements(
            String query,
            AsyncCallback<List<Announcement>> callback);
}

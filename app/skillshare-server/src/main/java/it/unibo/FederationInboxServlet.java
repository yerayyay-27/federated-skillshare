package it.unibo;

import java.io.BufferedReader;
import java.io.IOException;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Receives federation events from peer instances (server-to-server).
 * This is a plain Jakarta HttpServlet, not a GWT RPC servlet, because the
 * caller is another instance sending JSON over HTTP, not a browser.
 */
@SuppressWarnings("serial")
public class FederationInboxServlet extends HttpServlet {

    private final Gson gson = new Gson();
    private final AnnouncementRepository announcementRepository = new AnnouncementRepository();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = readBody(req);
        FederationEvent event;
        try {
            event = gson.fromJson(body, FederationEvent.class);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed event");
            return;
        }

        if (event == null || event.getType() == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing event type");
            return;
        }

        if (FederationEvent.TYPE_ANNOUNCEMENT_CREATED.equals(event.getType())) {
            handleAnnouncementCreated(event);
        }
        // Unknown event types are ignored for now (forward compatibility).

        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void handleAnnouncementCreated(FederationEvent event) {
        Announcement remote = event.getAnnouncement();
        if (remote == null) {
            return;
        }
        // Store the remote announcement locally. save() ignores duplicates
        // (same id) atomically, which makes receiving the same event twice
        // harmless (idempotent delivery).
        announcementRepository.save(remote);
        System.out.println("Federation: stored announcement " + remote.getId()
                + " from " + event.getOriginInstance());
    }

    private String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
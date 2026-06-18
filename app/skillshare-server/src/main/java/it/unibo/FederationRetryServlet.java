package it.unibo;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Manual trigger for retrying persisted outgoing federation events.
 * This endpoint is intentionally server-side HTTP only; it is not part of the
 * GWT RPC API and does not add background scheduling.
 */
@SuppressWarnings("serial")
public class FederationRetryServlet extends HttpServlet {

    private final FederationClient federationClient;

    public FederationRetryServlet() {
        this(new FederationClient());
    }

    FederationRetryServlet(FederationClient federationClient) {
        if (federationClient == null) {
            throw new IllegalArgumentException("Federation client must not be null");
        }
        this.federationClient = federationClient;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int delivered = federationClient.retryPending();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"status\":\"ok\",\"delivered\":" + delivered + "}");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Use POST to retry federation outbox events");
    }
}

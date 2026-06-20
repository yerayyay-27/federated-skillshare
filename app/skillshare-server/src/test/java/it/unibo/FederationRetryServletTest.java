package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class FederationRetryServletTest {

    @Test
    void postRetriesPendingOutboxEvents() throws Exception {
        TestFederationClient client = new TestFederationClient(3);
        FederationRetryServlet servlet = new FederationRetryServlet(client);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        servlet.doPost(request, response);

        assertEquals(1, client.getRetryCalls());
        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        assertEquals("{\"status\":\"ok\",\"delivered\":3}", body.toString());
    }

    @Test
    void getIsRejected() throws Exception {
        FederationRetryServlet servlet = new FederationRetryServlet(new TestFederationClient(0));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doGet(request, response);

        verify(response).sendError(
                HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "Use POST to retry federation outbox events");
    }

    private static class TestFederationClient extends FederationClient {

        private final int delivered;
        private int retryCalls;

        TestFederationClient(int delivered) {
            super(() -> Collections.emptyList(), null);
            this.delivered = delivered;
        }

        @Override
        public int retryPending() {
            retryCalls++;
            return delivered;
        }

        int getRetryCalls() {
            return retryCalls;
        }
    }
}

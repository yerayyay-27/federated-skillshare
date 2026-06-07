package it.unibo;

import java.util.List;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class ExchangeServiceImpl extends RemoteServiceServlet implements ExchangeService {

    private final ExchangeRequestManager exchangeManager;

    public ExchangeServiceImpl() {
        this(new ExchangeRequestManager());
    }

    ExchangeServiceImpl(ExchangeRequestManager exchangeManager) {
        if (exchangeManager == null) {
            throw new IllegalArgumentException("Exchange manager must not be null");
        }
        this.exchangeManager = exchangeManager;
    }

    @Override
    public ExchangeRequest createRequest(String announcementId, String fromUsername, String message)
            throws IllegalArgumentException {
        return exchangeManager.createRequest(announcementId, fromUsername, message);
    }

    @Override
    public List<ExchangeRequest> getReceivedRequests(String username) {
        return exchangeManager.getReceivedRequests(username);
    }

    @Override
    public List<ExchangeRequest> getSentRequests(String username) {
        return exchangeManager.getSentRequests(username);
    }

    @Override
    public boolean acceptRequest(String id) {
        return exchangeManager.acceptRequest(id);
    }

    @Override
    public boolean rejectRequest(String id) {
        return exchangeManager.rejectRequest(id);
    }
}
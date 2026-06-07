package it.unibo;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("exchanges")
public interface ExchangeService extends RemoteService {

    ExchangeRequest createRequest(String announcementId, String fromUsername, String message)
            throws IllegalArgumentException;

    List<ExchangeRequest> getReceivedRequests(String username);

    List<ExchangeRequest> getSentRequests(String username);

    boolean acceptRequest(String id);

    boolean rejectRequest(String id);
}
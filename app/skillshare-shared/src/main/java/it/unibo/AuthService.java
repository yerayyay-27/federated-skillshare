package it.unibo;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("auth")
public interface AuthService extends RemoteService {
    User login(String email, String password) throws IllegalArgumentException;
    User register(String username, String email, String password) throws IllegalArgumentException;
}
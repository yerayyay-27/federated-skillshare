package it.unibo;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("auth")
public interface AuthService extends RemoteService {
    // Login no longer throws on bad credentials: it returns a LoginResult that
    // is either a success (with the user) or a failure (with a readable reason),
    // so the client can show the reason instead of a generic error.
    LoginResult login(String email, String password);

    User register(String username, String email, String password) throws IllegalArgumentException;
}
package it.unibo;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface AuthServiceAsync {
    void login(String email, String password, AsyncCallback<LoginResult> callback);
    void register(String username, String email, String password, AsyncCallback<User> callback);
}
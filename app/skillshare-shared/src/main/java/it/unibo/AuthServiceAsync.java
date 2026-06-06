package it.unibo;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface AuthServiceAsync {
    void login(String email, String password, AsyncCallback<Utente> callback);
    void registra(String username, String email, String password, AsyncCallback<Utente> callback);
}
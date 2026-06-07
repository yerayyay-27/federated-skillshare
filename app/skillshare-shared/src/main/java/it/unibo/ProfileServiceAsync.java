package it.unibo;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ProfileServiceAsync {
    void updateProfile(String email, String bio, List<String> skillTags,
            AsyncCallback<User> callback);
}
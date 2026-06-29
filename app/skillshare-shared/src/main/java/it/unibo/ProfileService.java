package it.unibo;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("profile")
public interface ProfileService extends RemoteService {
    User updateProfile(String email, String bio, List<String> skillTags)
            throws IllegalArgumentException;

    User updatePhoto(String email, String photo) throws IllegalArgumentException;

    // Public read-only profile of another user, looked up by username.
    // Returns null if no local user has that username.
    User getProfileByUsername(String username);
}
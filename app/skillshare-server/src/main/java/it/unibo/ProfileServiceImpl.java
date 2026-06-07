package it.unibo;

import java.util.List;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class ProfileServiceImpl extends RemoteServiceServlet implements ProfileService {

    @Override
    public User updateProfile(String email, String bio, List<String> skillTags)
            throws IllegalArgumentException {
        if (email == null || email.trim().isEmpty()
                || !UserRepository.exists(email.trim())) {
            throw new IllegalArgumentException("User not found");
        }
        String key = email.trim();
        UserRepository.updateProfile(key, bio, skillTags);
        return UserRepository.getUser(key);
    }
}

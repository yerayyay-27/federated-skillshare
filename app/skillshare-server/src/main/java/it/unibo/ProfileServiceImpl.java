package it.unibo;

import java.util.List;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class ProfileServiceImpl extends RemoteServiceServlet implements ProfileService {

    private final UserRepository userRepository;

    public ProfileServiceImpl() {
        this(new UserRepository());
    }

    // package-private constructor for tests (inject an in-memory repository)
    ProfileServiceImpl(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException("User repository must not be null");
        }
        this.userRepository = userRepository;
    }

    @Override
    public User updateProfile(String email, String bio, List<String> skillTags)
            throws IllegalArgumentException {
        if (email == null || email.trim().isEmpty()
                || !userRepository.exists(email.trim())) {
            throw new IllegalArgumentException("User not found");
        }
        String key = email.trim();
        userRepository.updateProfile(key, bio, skillTags);
        return userRepository.getUser(key);
    }
}
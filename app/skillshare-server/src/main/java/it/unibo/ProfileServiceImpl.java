package it.unibo;

import java.util.List;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class ProfileServiceImpl extends RemoteServiceServlet implements ProfileService {

    // ~360 KB image once base64-encoded; keeps the User object light
    private static final int MAX_PHOTO_CHARS = 500_000;

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

    @Override
    public User updatePhoto(String email, String photo) throws IllegalArgumentException {
        if (email == null || email.trim().isEmpty()
                || !userRepository.exists(email.trim())) {
            throw new IllegalArgumentException("User not found");
        }
        if (photo != null && !photo.isEmpty()) {
            if (!photo.startsWith("data:image/")) {
                throw new IllegalArgumentException("Invalid image format");
            }
            if (photo.length() > MAX_PHOTO_CHARS) {
                throw new IllegalArgumentException(
                        "Image is too large (max ~360 KB). Please use a smaller image.");
            }
        }
        String key = email.trim();
        userRepository.updatePhoto(key, photo);
        return userRepository.getUser(key);
    }

    @Override
    public User getProfileByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        // getUser/findByUsername never expose the password, so the returned
        // User is safe to show as a public profile.
        return userRepository.findByUsername(username.trim());
    }
}
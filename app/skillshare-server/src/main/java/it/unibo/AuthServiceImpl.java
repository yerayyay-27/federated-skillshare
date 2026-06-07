package it.unibo;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class AuthServiceImpl extends RemoteServiceServlet implements AuthService {

    private final UserRepository userRepository;

    public AuthServiceImpl() {
        this(new UserRepository());
    }

    // package-private constructor for tests (inject an in-memory repository)
    AuthServiceImpl(UserRepository userRepository) {
        if (userRepository == null) {
            throw new IllegalArgumentException("User repository must not be null");
        }
        this.userRepository = userRepository;
    }

    @Override
    public User login(String email, String password) throws IllegalArgumentException {
        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        String key = email.trim();
        if (!userRepository.checkPassword(key, password)) {
            throw new IllegalArgumentException("Wrong email or password");
        }
        return userRepository.getUser(key);
    }

    @Override
    public User register(String username, String email, String password)
            throws IllegalArgumentException {
        if (username == null || username.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.length() < 4) {
            throw new IllegalArgumentException(
                "Invalid data: the password must be at least 4 characters long");
        }
        String key = email.trim();
        if (userRepository.exists(key)) {
            throw new IllegalArgumentException("A user with this email already exists");
        }
        userRepository.create(key, username.trim(), password);
        return userRepository.getUser(key);
    }
}
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
    public LoginResult login(String email, String password) {
        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {
            return LoginResult.failure("Please enter both email and password.");
        }
        String key = email.trim();
        // Distinguish the two failure cases so the user gets a clear reason.
        if (!userRepository.exists(key)) {
            return LoginResult.failure("No account found for this email.");
        }
        if (!userRepository.checkPassword(key, password)) {
            return LoginResult.failure("Incorrect password.");
        }
        return LoginResult.success(userRepository.getUser(key));
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
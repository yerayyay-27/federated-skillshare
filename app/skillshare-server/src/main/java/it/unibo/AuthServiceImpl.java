package it.unibo;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class AuthServiceImpl extends RemoteServiceServlet implements AuthService {

    @Override
    public User login(String email, String password) throws IllegalArgumentException {
        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        String key = email.trim();
        if (!UserRepository.checkPassword(key, password)) {
            throw new IllegalArgumentException("Wrong email or password");
        }
        return UserRepository.getUser(key);
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
        if (UserRepository.exists(key)) {
            throw new IllegalArgumentException("A user with this email already exists");
        }
        UserRepository.create(key, username.trim(), password);
        return UserRepository.getUser(key);
    }
}
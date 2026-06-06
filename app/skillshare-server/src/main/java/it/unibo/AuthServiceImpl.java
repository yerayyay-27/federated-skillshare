package it.unibo;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class AuthServiceImpl extends RemoteServiceServlet implements AuthService {

    // In-memory storage (temporary, until a database is added)
    // key = email, value = [password, username]
    private final Map<String, String[]> users = new HashMap<String, String[]>();

    public AuthServiceImpl() {
        // test user to try the login right away
        users.put("test@unibo.it", new String[] { "1234", "TestUser" });
    }

    @Override
    public User login(String email, String password) throws IllegalArgumentException {
        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        String[] userData = users.get(email.trim());
        if (userData == null || !userData[0].equals(password)) {
            throw new IllegalArgumentException("Wrong email or password");
        }
        return new User(userData[1], email.trim());
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
        if (users.containsKey(email.trim())) {
            throw new IllegalArgumentException("A user with this email already exists");
        }
        users.put(email.trim(), new String[] { password, username.trim() });
        return new User(username.trim(), email.trim());
    }
}
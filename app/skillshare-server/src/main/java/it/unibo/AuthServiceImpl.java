package it.unibo;

import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.Serializer;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

@SuppressWarnings("serial")
public class AuthServiceImpl extends RemoteServiceServlet implements AuthService {

    private static final DB db = DatabaseCore.getDB();

    // Persistent storage: email -> password
    private static final ConcurrentMap<String, String> passwords =
            db.hashMap("passwords", Serializer.STRING, Serializer.STRING).createOrOpen();

    // Persistent storage: email -> username
    private static final ConcurrentMap<String, String> usernames =
            db.hashMap("usernames", Serializer.STRING, Serializer.STRING).createOrOpen();

    // Seed a test user on first run (only if it doesn't exist yet)
    static {
        if (!passwords.containsKey("test@unibo.it")) {
            passwords.put("test@unibo.it", "1234");
            usernames.put("test@unibo.it", "TestUser");
            DatabaseCore.commit();
        }
    }

    @Override
    public User login(String email, String password) throws IllegalArgumentException {
        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email and password are required");
        }
        String key = email.trim();
        String storedPassword = passwords.get(key);
        if (storedPassword == null || !storedPassword.equals(password)) {
            throw new IllegalArgumentException("Wrong email or password");
        }
        return new User(usernames.get(key), key);
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
        if (passwords.containsKey(key)) {
            throw new IllegalArgumentException("A user with this email already exists");
        }
        passwords.put(key, password);
        usernames.put(key, username.trim());
        // Persist the changes to disk
        DatabaseCore.commit();
        return new User(username.trim(), key);
    }
}
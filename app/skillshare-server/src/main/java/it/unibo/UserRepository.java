package it.unibo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.Serializer;

/**
 * Centralized data access for users, backed by MapDB.
 * All services that need user data go through this class.
 */
public class UserRepository {

    private static final DB db = DatabaseCore.getDB();

    private static final ConcurrentMap<String, String> passwords =
            db.hashMap("passwords", Serializer.STRING, Serializer.STRING).createOrOpen();
    private static final ConcurrentMap<String, String> usernames =
            db.hashMap("usernames", Serializer.STRING, Serializer.STRING).createOrOpen();
    private static final ConcurrentMap<String, String> bios =
            db.hashMap("bios", Serializer.STRING, Serializer.STRING).createOrOpen();
    // skill tags stored as a single comma-separated string per user
    private static final ConcurrentMap<String, String> skillTags =
            db.hashMap("skillTags", Serializer.STRING, Serializer.STRING).createOrOpen();

    static {
        // seed a test user on first run
        if (!passwords.containsKey("test@unibo.it")) {
            passwords.put("test@unibo.it", "1234");
            usernames.put("test@unibo.it", "TestUser");
            DatabaseCore.commit();
        }
    }

    public static boolean exists(String email) {
        return passwords.containsKey(email);
    }

    public static boolean checkPassword(String email, String password) {
        String stored = passwords.get(email);
        return stored != null && stored.equals(password);
    }

    public static void create(String email, String username, String password) {
        passwords.put(email, password);
        usernames.put(email, username);
        DatabaseCore.commit();
    }

    public static void updateProfile(String email, String bio, List<String> tags) {
        bios.put(email, bio == null ? "" : bio);
        skillTags.put(email, tags == null ? "" : String.join(",", tags));
        DatabaseCore.commit();
    }

    // Rebuilds the full User object from the separate collections
    public static User getUser(String email) {
        if (!usernames.containsKey(email)) {
            return null;
        }
        User user = new User(usernames.get(email), email);
        String bio = bios.get(email);
        user.setBio(bio == null ? "" : bio);
        String tags = skillTags.get(email);
        if (tags != null && !tags.isEmpty()) {
            user.setSkillTags(new ArrayList<String>(Arrays.asList(tags.split(","))));
        }
        return user;
    }
}
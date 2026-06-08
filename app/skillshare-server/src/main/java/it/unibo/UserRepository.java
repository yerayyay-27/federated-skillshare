package it.unibo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.mapdb.DB;
import org.mapdb.Serializer;

/**
 * Centralized data access for users, backed by MapDB.
 * Collections are opened in the constructor (not in static fields) so that
 * DatabaseCore.enableTestMode() can swap the database to an in-memory one
 * BEFORE the repository is created, allowing isolated unit tests.
 */
public class UserRepository {

    private final ConcurrentMap<String, String> passwords;
    private final ConcurrentMap<String, String> usernames;
    private final ConcurrentMap<String, String> bios;
    // skill tags stored as a single comma-separated string per user
    private final ConcurrentMap<String, String> skillTags;
    // profile photo stored as a base64 data URL per user
    private final ConcurrentMap<String, String> photos;

    public UserRepository() {
        DB db = DatabaseCore.getDB();
        passwords = db.hashMap("passwords", Serializer.STRING, Serializer.STRING).createOrOpen();
        usernames = db.hashMap("usernames", Serializer.STRING, Serializer.STRING).createOrOpen();
        bios = db.hashMap("bios", Serializer.STRING, Serializer.STRING).createOrOpen();
        skillTags = db.hashMap("skillTags", Serializer.STRING, Serializer.STRING).createOrOpen();
        photos = db.hashMap("photos", Serializer.STRING, Serializer.STRING).createOrOpen();

        // seed a test user on first run (only if it doesn't exist yet)
        if (!passwords.containsKey("test@unibo.it")) {
            passwords.put("test@unibo.it", "1234");
            usernames.put("test@unibo.it", "TestUser");
            DatabaseCore.commit();
        }
    }

    public boolean exists(String email) {
        return passwords.containsKey(email);
    }

    public boolean checkPassword(String email, String password) {
        String stored = passwords.get(email);
        return stored != null && stored.equals(password);
    }

    public void create(String email, String username, String password) {
        passwords.put(email, password);
        usernames.put(email, username);
        DatabaseCore.commit();
    }

    public void updateProfile(String email, String bio, List<String> tags) {
        bios.put(email, bio == null ? "" : bio);
        skillTags.put(email, tags == null ? "" : String.join(",", tags));
        DatabaseCore.commit();
    }

    public void updatePhoto(String email, String photo) {
        photos.put(email, photo == null ? "" : photo);
        DatabaseCore.commit();
    }

    // Rebuilds the full User object from the separate collections
    public User getUser(String email) {
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
        String photo = photos.get(email);
        user.setPhoto(photo == null ? "" : photo);
        return user;
    }
}
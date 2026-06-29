package it.unibo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    private final ConcurrentMap<String, String> skillTags;
    private final ConcurrentMap<String, String> photos;

    public UserRepository() {
        DB db = DatabaseCore.getDB();
        passwords = db.hashMap("passwords", Serializer.STRING, Serializer.STRING).createOrOpen();
        usernames = db.hashMap("usernames", Serializer.STRING, Serializer.STRING).createOrOpen();
        bios = db.hashMap("bios", Serializer.STRING, Serializer.STRING).createOrOpen();
        skillTags = db.hashMap("skillTags", Serializer.STRING, Serializer.STRING).createOrOpen();
        photos = db.hashMap("photos", Serializer.STRING, Serializer.STRING).createOrOpen();

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

    // Rebuilds the full User object from the separate collections.
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
        // Federated identity: every user belongs to this instance.
        user.setInstance(FederationConfig.get().getInstanceId());
        return user;
    }

    // Looks up a user by their display username (usernames are stored as
    // email -> username). Used to show another user's public profile. Returns
    // the first match, or null if no local user has that username. Usernames
    // are not guaranteed unique across emails, which mirrors how reputation is
    // already keyed by username.
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        String target = username.trim();
        for (Map.Entry<String, String> entry : usernames.entrySet()) {
            if (target.equals(entry.getValue())) {
                return getUser(entry.getKey()); // key is the email
            }
        }
        return null;
    }
}
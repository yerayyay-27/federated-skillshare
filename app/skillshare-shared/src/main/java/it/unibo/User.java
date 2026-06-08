package it.unibo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private String username;
    private String email;
    private String bio;
    private List<String> skillTags;
    private String photo; // base64 data URL, empty if none

    public User() { // required for GWT serialization
        this.bio = "";
        this.skillTags = new ArrayList<String>();
        this.photo = "";
    }

    public User(String username, String email) {
        this();
        this.username = username;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getSkillTags() { return skillTags; }
    public void setSkillTags(List<String> skillTags) { this.skillTags = skillTags; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }
}
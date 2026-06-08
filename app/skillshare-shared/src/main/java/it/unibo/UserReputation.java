package it.unibo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserReputation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private double averageRating;
    private int reviewCount;
    private List<Review> reviews;

    public UserReputation() {
        this.reviews = new ArrayList<Review>();
    }

    public UserReputation(String username, double averageRating, int reviewCount, List<Review> reviews) {
        this.username = username;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.reviews = reviews;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }
}
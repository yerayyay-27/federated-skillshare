package it.unibo;

import java.io.Serializable;

public class Review implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String exchangeRequestId;
    private String fromUsername;   // who writes the review
    private String toUsername;     // who is being reviewed
    private String fromInstance;
    private String toInstance;
    private int rating;            // 1..5
    private String comment;
    private long timestamp;

    public Review() {
    }

    public Review(String id, String exchangeRequestId, String fromUsername,
            String toUsername, int rating, String comment, long timestamp) {
        this.id = id;
        this.exchangeRequestId = exchangeRequestId;
        this.fromUsername = fromUsername;
        this.toUsername = toUsername;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExchangeRequestId() { return exchangeRequestId; }
    public void setExchangeRequestId(String exchangeRequestId) { this.exchangeRequestId = exchangeRequestId; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }

    public String getFromInstance() { return fromInstance; }
    public void setFromInstance(String fromInstance) { this.fromInstance = fromInstance; }

    public String getToInstance() { return toInstance; }
    public void setToInstance(String toInstance) { this.toInstance = toInstance; }

    public String getFromHandle() {
        return fromInstance == null ? fromUsername : fromUsername + "@" + fromInstance;
    }

    public String getToHandle() {
        return toInstance == null ? toUsername : toUsername + "@" + toInstance;
    }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

package it.unibo;

import java.io.Serializable;

public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String exchangeRequestId;
    private String senderUsername;
    private String text;
    private long timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String exchangeRequestId, String senderUsername, String text, long timestamp) {
        this.exchangeRequestId = exchangeRequestId;
        this.senderUsername = senderUsername;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getExchangeRequestId() { return exchangeRequestId; }
    public void setExchangeRequestId(String exchangeRequestId) { this.exchangeRequestId = exchangeRequestId; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
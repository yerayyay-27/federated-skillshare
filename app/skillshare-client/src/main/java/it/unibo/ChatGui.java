package it.unibo;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ChatGui {

    private final ChatServiceAsync chatService = GWT.create(ChatService.class);
    private final User currentUser;
    private final ExchangeRequest exchangeRequest;

    private VerticalPanel messagesPanel;
    private Label statusLabel;

    public ChatGui(User currentUser, ExchangeRequest exchangeRequest) {
        this.currentUser = currentUser;
        this.exchangeRequest = exchangeRequest;
    }

    public void show() {
        RootPanel.get().clear();

        String otherUser = otherParticipant();
        HTML title = new HTML("<h1>Skillshare - Chat</h1>");
        HTML subtitle = new HTML("<p>Conversation with <b>" + otherUser
                + "</b> about <b>" + displayValue(exchangeRequest.getAnnouncementOfferedSkill())
                + "</b></p>");

        statusLabel = new Label();
        statusLabel.addStyleName("serverResponseLabelError");

        messagesPanel = new VerticalPanel();
        messagesPanel.setSpacing(6);
        messagesPanel.setWidth("100%");

        final TextBox messageField = new TextBox();
        messageField.getElement().setPropertyString("placeholder", "Type a message...");
        messageField.setWidth("300px");

        final Button sendButton = new Button("Send");
        final Button refreshButton = new Button("Refresh");
        final Button backButton = new Button("Back to requests");

        HorizontalPanel inputPanel = new HorizontalPanel();
        inputPanel.setSpacing(6);
        inputPanel.add(messageField);
        inputPanel.add(sendButton);

        HorizontalPanel actionsPanel = new HorizontalPanel();
        actionsPanel.setSpacing(6);
        actionsPanel.add(refreshButton);
        actionsPanel.add(backButton);

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(12);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        mainPanel.add(title);
        mainPanel.add(subtitle);
        mainPanel.add(statusLabel);
        mainPanel.add(messagesPanel);
        mainPanel.add(inputPanel);
        mainPanel.add(actionsPanel);

        RootPanel.get().add(mainPanel);
        messageField.setFocus(true);

        class SendHandler implements ClickHandler, KeyUpHandler {
            public void onClick(ClickEvent event) {
                sendMessage(messageField);
            }
            public void onKeyUp(KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    sendMessage(messageField);
                }
            }
        }
        SendHandler sendHandler = new SendHandler();
        sendButton.addClickHandler(sendHandler);
        messageField.addKeyUpHandler(sendHandler);

        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                loadMessages();
            }
        });

        backButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new ExchangeRequestsGui(currentUser).show();
            }
        });

        loadMessages();
    }

    private void sendMessage(final TextBox messageField) {
        final String text = messageField.getText();
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        statusLabel.setText("Sending...");
        chatService.sendMessage(
                exchangeRequest.getId(),
                currentUser.getUsername(),
                text,
                new AsyncCallback<ChatMessage>() {
                    public void onFailure(Throwable caught) {
                        statusLabel.setText("Unable to send: " + caught.getMessage());
                    }
                    public void onSuccess(ChatMessage message) {
                        statusLabel.setText("");
                        messageField.setText("");
                        loadMessages();
                    }
                });
    }

    private void loadMessages() {
        statusLabel.setText("Loading messages...");
        chatService.getMessages(
                exchangeRequest.getId(),
                currentUser.getUsername(),
                new AsyncCallback<List<ChatMessage>>() {
                    public void onFailure(Throwable caught) {
                        statusLabel.setText("Unable to load messages: " + caught.getMessage());
                    }
                    public void onSuccess(List<ChatMessage> messages) {
                        statusLabel.setText("");
                        renderMessages(messages);
                    }
                });
    }

    private void renderMessages(List<ChatMessage> messages) {
        messagesPanel.clear();
        if (messages == null || messages.isEmpty()) {
            messagesPanel.add(new Label("No messages yet. Say hello!"));
            return;
        }
        DateTimeFormat timeFormat = DateTimeFormat.getFormat(
                DateTimeFormat.PredefinedFormat.HOUR24_MINUTE);
        for (ChatMessage message : messages) {
            String time = timeFormat.format(new Date(message.getTimestamp()));
            messagesPanel.add(new Label(
                    "[" + time + "] " + message.getSenderUsername() + ": " + message.getText()));
        }
    }

    private String otherParticipant() {
        // show the participant who is NOT the current user
        if (currentUser.getUsername() != null
                && currentUser.getUsername().equals(exchangeRequest.getFromUsername())) {
            return exchangeRequest.getToUsername();
        }
        return exchangeRequest.getFromUsername();
    }

    private String displayValue(String value) {
        return value == null || value.trim().isEmpty() ? "(skill)" : value;
    }
}
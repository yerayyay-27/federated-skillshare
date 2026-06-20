package it.unibo;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ExchangeRequestsGui {

    private final ExchangeServiceAsync exchangeService = GWT.create(ExchangeService.class);
    private final User currentUser;

    private VerticalPanel receivedPanel;
    private VerticalPanel sentPanel;
    private Label statusLabel;

    public ExchangeRequestsGui(User currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Exchange requests</h1>");
        statusLabel = new Label();
        statusLabel.addStyleName("serverResponseLabelError");

        receivedPanel = new VerticalPanel();
        receivedPanel.setSpacing(8);
        receivedPanel.setWidth("100%");

        sentPanel = new VerticalPanel();
        sentPanel.setSpacing(8);
        sentPanel.setWidth("100%");

        final Button backButton = new Button("Back to home");

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(12);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        mainPanel.add(title);
        mainPanel.add(statusLabel);
        mainPanel.add(new HTML("<h2>Received</h2>"));
        mainPanel.add(receivedPanel);
        mainPanel.add(new HTML("<h2>Sent</h2>"));
        mainPanel.add(sentPanel);
        mainPanel.add(backButton);

        RootPanel.get().add(mainPanel);

        backButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new HomeGui(currentUser).show();
            }
        });

        loadRequests();
    }

    private void loadRequests() {
        statusLabel.setText("Loading requests...");

        exchangeService.getReceivedRequests(currentUser.getUsername(),
                new AsyncCallback<List<ExchangeRequest>>() {
                    public void onFailure(Throwable caught) {
                        statusLabel.setText("Unable to load received requests: " + caught.getMessage());
                    }
                    public void onSuccess(List<ExchangeRequest> requests) {
                        statusLabel.setText("");
                        renderReceived(requests);
                    }
                });

        exchangeService.getSentRequests(currentUser.getUsername(),
                new AsyncCallback<List<ExchangeRequest>>() {
                    public void onFailure(Throwable caught) {
                        statusLabel.setText("Unable to load sent requests: " + caught.getMessage());
                    }
                    public void onSuccess(List<ExchangeRequest> requests) {
                        renderSent(requests);
                    }
                });
    }

    private void renderReceived(List<ExchangeRequest> requests) {
        receivedPanel.clear();
        if (requests == null || requests.isEmpty()) {
            receivedPanel.add(new Label("No received requests."));
            return;
        }
        for (final ExchangeRequest request : requests) {
            VerticalPanel card = new VerticalPanel();
            card.setSpacing(4);
            card.add(new Label("From: " + request.getFromHandle()));
            card.add(new Label("Skill: " + request.getAnnouncementOfferedSkill()));
            card.add(new Label("Message: " + displayValue(request.getMessage())));
            card.add(new Label("Status: " + request.getStatus()));

            if (ExchangeRequest.STATUS_PENDING.equals(request.getStatus())) {
                Button acceptButton = new Button("Accept");
                acceptButton.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        respond(request.getId(), true);
                    }
                });
                Button rejectButton = new Button("Reject");
                rejectButton.addClickHandler(new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        respond(request.getId(), false);
                    }
                });
                card.add(acceptButton);
                card.add(rejectButton);
            } else if (ExchangeRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
                card.add(chatButton(request));
                card.add(reviewButton(request));
            }
            receivedPanel.add(card);
        }
    }

    private void renderSent(List<ExchangeRequest> requests) {
        sentPanel.clear();
        if (requests == null || requests.isEmpty()) {
            sentPanel.add(new Label("No sent requests."));
            return;
        }
        for (final ExchangeRequest request : requests) {
            VerticalPanel card = new VerticalPanel();
            card.setSpacing(4);
            card.add(new Label("To: " + request.getToHandle()));
            card.add(new Label("Skill: " + request.getAnnouncementOfferedSkill()));
            card.add(new Label("Status: " + request.getStatus()));

            if (ExchangeRequest.STATUS_ACCEPTED.equals(request.getStatus())) {
                card.add(chatButton(request));
                card.add(reviewButton(request));
            }
            sentPanel.add(card);
        }
    }

    private Button chatButton(final ExchangeRequest request) {
        Button openChatButton = new Button("Open chat");
        openChatButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new ChatGui(currentUser, request).show();
            }
        });
        return openChatButton;
    }

    private Button reviewButton(final ExchangeRequest request) {
        Button button = new Button("Leave a review");
        button.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new ReviewGui(currentUser, request).show();
            }
        });
        return button;
    }

    private void respond(String requestId, final boolean accept) {
        statusLabel.setText(accept ? "Accepting..." : "Rejecting...");
        AsyncCallback<Boolean> callback = new AsyncCallback<Boolean>() {
            public void onFailure(Throwable caught) {
                statusLabel.setText("Operation failed: " + caught.getMessage());
            }
            public void onSuccess(Boolean ok) {
                if (Boolean.TRUE.equals(ok)) {
                    loadRequests();
                } else {
                    statusLabel.setText("The request could not be updated.");
                }
            }
        };
        if (accept) {
            exchangeService.acceptRequest(requestId, callback);
        } else {
            exchangeService.rejectRequest(requestId, callback);
        }
    }

    private String displayValue(String value) {
        return value == null || value.trim().isEmpty() ? "(none)" : value;
    }
}

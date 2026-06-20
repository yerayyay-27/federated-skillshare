package it.unibo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ReviewGui {

    private final ReviewServiceAsync reviewService = GWT.create(ReviewService.class);
    private final User currentUser;
    private final ExchangeRequest exchangeRequest;

    public ReviewGui(User currentUser, ExchangeRequest exchangeRequest) {
        this.currentUser = currentUser;
        this.exchangeRequest = exchangeRequest;
    }

    public void show() {
        RootPanel.get().clear();

        String target = otherParticipant();
        HTML title = new HTML("<h1>Skillshare - Leave a review</h1>");
        HTML subtitle = new HTML("<p>Reviewing <b>" + target + "</b></p>");
        final Label statusLabel = new Label();
        statusLabel.addStyleName("serverResponseLabelError");

        final ListBox ratingBox = new ListBox();
        for (int i = 5; i >= 1; i--) {
            ratingBox.addItem(i + " star" + (i > 1 ? "s" : ""), String.valueOf(i));
        }

        final TextArea commentField = new TextArea();
        commentField.setCharacterWidth(40);
        commentField.setVisibleLines(4);
        commentField.getElement().setPropertyString("placeholder", "Write a comment (optional)...");

        final Button submitButton = new Button("Submit review");
        final Button backButton = new Button("Back to requests");

        final VerticalPanel formPanel = new VerticalPanel();
        formPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        formPanel.setSpacing(10);
        formPanel.add(new HTML("<b>Rating:</b>"));
        formPanel.add(ratingBox);
        formPanel.add(new HTML("<b>Comment:</b>"));
        formPanel.add(commentField);
        formPanel.add(submitButton);

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        mainPanel.add(title);
        mainPanel.add(subtitle);
        mainPanel.add(statusLabel);
        mainPanel.add(formPanel);
        mainPanel.add(backButton);

        RootPanel.get().add(mainPanel);

        // The form starts hidden until we confirm the user is allowed to review
        formPanel.setVisible(false);
        statusLabel.setText("Checking...");

        reviewService.getReviewBlockReason(exchangeRequest.getId(), currentUser.getUsername(),
                new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) {
                        statusLabel.setText("Error: " + caught.getMessage());
                    }
                    public void onSuccess(String reason) {
                        if (reason != null && !reason.isEmpty()) {
                            // Not allowed: explain why and keep the form hidden
                            statusLabel.setText(reason);
                        } else {
                            statusLabel.setText("");
                            formPanel.setVisible(true);
                        }
                    }
                });

        submitButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                int rating = Integer.parseInt(ratingBox.getSelectedValue());
                submitButton.setEnabled(false);
                statusLabel.setText("");
                reviewService.createReview(
                        exchangeRequest.getId(),
                        currentUser.getUsername(),
                        rating,
                        commentField.getText(),
                        new AsyncCallback<Review>() {
                            public void onFailure(Throwable caught) {
                                submitButton.setEnabled(true);
                                statusLabel.setText("Error: " + caught.getMessage());
                            }
                            public void onSuccess(Review review) {
                                Window.alert("Review submitted. Thank you!");
                                new ExchangeRequestsGui(currentUser).show();
                            }
                        });
            }
        });

        backButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new ExchangeRequestsGui(currentUser).show();
            }
        });
    }

    private String otherParticipant() {
        if (currentUser.getUsername() != null
                && currentUser.getUsername().equals(exchangeRequest.getFromUsername())
                && sameInstance(currentUser.getInstance(), exchangeRequest.getFromInstance())) {
            return exchangeRequest.getToHandle();
        }
        return exchangeRequest.getFromHandle();
    }

    private boolean sameInstance(String first, String second) {
        return first == null || second == null || first.equals(second);
    }
}

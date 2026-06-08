package it.unibo;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ReputationGui {

    private final ReviewServiceAsync reviewService = GWT.create(ReviewService.class);
    private final User currentUser;
    private final String targetUsername;

    public ReputationGui(User currentUser, String targetUsername) {
        this.currentUser = currentUser;
        this.targetUsername = targetUsername;
    }

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Reputation</h1>");
        HTML subtitle = new HTML("<h2>" + targetUsername + "</h2>");
        final Label summaryLabel = new Label("Loading reputation...");
        final VerticalPanel reviewsPanel = new VerticalPanel();
        reviewsPanel.setSpacing(8);
        reviewsPanel.setWidth("100%");
        final Button backButton = new Button("Back to home");

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(12);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        mainPanel.add(title);
        mainPanel.add(subtitle);
        mainPanel.add(summaryLabel);
        mainPanel.add(reviewsPanel);
        mainPanel.add(backButton);

        RootPanel.get().add(mainPanel);

        backButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new HomeGui(currentUser).show();
            }
        });

        reviewService.getReputation(targetUsername, new AsyncCallback<UserReputation>() {
            public void onFailure(Throwable caught) {
                summaryLabel.setText("Unable to load reputation: " + caught.getMessage());
            }
            public void onSuccess(UserReputation reputation) {
                render(summaryLabel, reviewsPanel, reputation);
            }
        });
    }

    private void render(Label summaryLabel, VerticalPanel reviewsPanel, UserReputation reputation) {
        if (reputation == null || reputation.getReviewCount() == 0) {
            summaryLabel.setText("No reviews yet.");
            return;
        }
        String avg = NumberFormat.getFormat("0.0").format(reputation.getAverageRating());
        summaryLabel.setText("Average rating: " + avg + " / 5  ("
                + reputation.getReviewCount() + " reviews)");

        List<Review> reviews = reputation.getReviews();
        for (Review review : reviews) {
            VerticalPanel card = new VerticalPanel();
            card.setSpacing(2);
            card.add(new Label(review.getRating() + "/5 - by " + review.getFromUsername()));
            String comment = review.getComment();
            if (comment != null && !comment.trim().isEmpty()) {
                card.add(new Label(comment));
            }
            reviewsPanel.add(card);
        }
    }
}
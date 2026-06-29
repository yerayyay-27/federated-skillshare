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
    // Where the "Back" button goes, with a label naming the destination. If
    // null, defaults to the home screen (used when reputation is opened from
    // Home). Other screens (e.g. a user's profile) pass their own back target.
    private final BackTarget backTarget;

    public ReputationGui(User currentUser, String targetUsername) {
        this(currentUser, targetUsername, null);
    }

    public ReputationGui(User currentUser, String targetUsername, BackTarget backTarget) {
        this.currentUser = currentUser;
        this.targetUsername = targetUsername;
        this.backTarget = backTarget;
    }

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Reputation</h1>");
        HTML subtitle = new HTML("<h2>" + targetUsername + "</h2>");
        final Label summaryLabel = new Label("Loading reputation...");
        final VerticalPanel reviewsPanel = new VerticalPanel();
        reviewsPanel.setSpacing(8);
        reviewsPanel.setWidth("100%");
        final Button backButton = new Button(
                backTarget == null ? "Back to home" : backTarget.getLabel());

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
                goBack();
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

    private void goBack() {
        if (backTarget != null) {
            backTarget.go();
        } else {
            new HomeGui(currentUser).show();
        }
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
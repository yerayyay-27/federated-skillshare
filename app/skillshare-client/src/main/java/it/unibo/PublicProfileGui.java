package it.unibo;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/** Read-only view of another user's profile, with a link to their reputation. */
public class PublicProfileGui {

    private final ProfileServiceAsync profileService = GWT.create(ProfileService.class);
    private final User currentUser;
    private final String targetUsername;
    // Where the "Back" button goes, with a label naming the destination. If
    // null, defaults to the marketplace (where this profile is normally opened
    // from). Another caller can pass its own back target.
    private final BackTarget backTarget;

    public PublicProfileGui(User currentUser, String targetUsername) {
        this(currentUser, targetUsername, null);
    }

    public PublicProfileGui(User currentUser, String targetUsername, BackTarget backTarget) {
        this.currentUser = currentUser;
        this.targetUsername = targetUsername;
        this.backTarget = backTarget;
    }

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Profile</h1>");
        final Label statusLabel = new Label("Loading profile...");

        final VerticalPanel contentPanel = new VerticalPanel();
        contentPanel.setSpacing(10);
        contentPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        final Button backButton = new Button(
                backTarget == null ? "Back to marketplace" : backTarget.getLabel());
        backButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                goBack();
            }
        });

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(12);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        mainPanel.add(title);
        mainPanel.add(statusLabel);
        mainPanel.add(contentPanel);
        mainPanel.add(backButton);

        RootPanel.get().add(mainPanel);

        profileService.getProfileByUsername(targetUsername, new AsyncCallback<User>() {
            public void onFailure(Throwable caught) {
                statusLabel.setText("Could not load this profile: " + caught.getMessage());
            }
            public void onSuccess(User profile) {
                if (profile == null) {
                    // Profiles are not federated yet, so a user from another
                    // instance has no local profile to show.
                    statusLabel.setText("This profile isn't available on this instance.");
                    return;
                }
                statusLabel.setText("");
                render(contentPanel, profile);
            }
        });
    }

    private void goBack() {
        if (backTarget != null) {
            backTarget.go();
        } else {
            new MarketplaceGui(currentUser).show();
        }
    }

    private void render(VerticalPanel panel, final User profile) {
        panel.clear();
        panel.add(new HTML("<h2>" + profile.getUsername() + "</h2>"));

        String photo = profile.getPhoto();
        if (photo != null && !photo.isEmpty()) {
            Image img = new Image();
            img.setWidth("120px");
            // Set src directly so base64 data URLs render correctly.
            img.getElement().setAttribute("src", photo);
            panel.add(img);
        }

        String bio = profile.getBio();
        panel.add(new Label("Bio: "
                + (bio == null || bio.trim().isEmpty() ? "(no bio)" : bio)));

        panel.add(new Label("Skills: " + joinTags(profile.getSkillTags())));

        Button reputationButton = new Button("View reputation");
        reputationButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                // Back from reputation returns to THIS profile, naming it, and
                // the profile keeps its own back target so the chain stays right.
                BackTarget backToProfile = new BackTarget(
                        "Back to " + profile.getUsername() + "'s profile",
                        new Runnable() {
                            public void run() {
                                new PublicProfileGui(currentUser, targetUsername, backTarget).show();
                            }
                        });
                new ReputationGui(currentUser, profile.getUsername(), backToProfile).show();
            }
        });
        panel.add(reputationButton);
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "(none)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(tags.get(i));
        }
        return sb.toString();
    }
}
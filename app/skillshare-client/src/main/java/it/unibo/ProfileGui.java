package it.unibo;

import java.util.ArrayList;
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
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ProfileGui {

    private final ProfileServiceAsync profileService = GWT.create(ProfileService.class);
    private final User currentUser;

    public ProfileGui(User currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - My Profile</h1>");
        HTML info = new HTML("<p><b>" + currentUser.getUsername()
                + "</b> (" + currentUser.getEmail() + ")</p>");

        final TextArea bioField = new TextArea();
        bioField.setCharacterWidth(40);
        bioField.setVisibleLines(4);
        bioField.getElement().setPropertyString("placeholder", "Write a short bio...");
        bioField.setText(currentUser.getBio());

        final TextBox tagsField = new TextBox();
        tagsField.getElement().setPropertyString("placeholder", "e.g. Java, Guitar, Cooking");
        tagsField.setWidth("300px");
        tagsField.setText(joinTags(currentUser.getSkillTags()));

        final Button saveButton = new Button("Save changes");
        final Button backButton = new Button("Back to home");
        final Label statusLabel = new Label();

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        mainPanel.add(title);
        mainPanel.add(info);
        mainPanel.add(new HTML("<b>Bio:</b>"));
        mainPanel.add(bioField);
        mainPanel.add(new HTML("<b>Skill tags (comma-separated):</b>"));
        mainPanel.add(tagsField);
        mainPanel.add(saveButton);
        mainPanel.add(statusLabel);
        mainPanel.add(backButton);

        RootPanel.get().add(mainPanel);

        // --- Save handler ---
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                statusLabel.setText("");
                final String bio = bioField.getText();
                final List<String> tags = parseTags(tagsField.getText());

                saveButton.setEnabled(false);
                profileService.updateProfile(currentUser.getEmail(), bio, tags,
                        new AsyncCallback<User>() {
                            public void onFailure(Throwable caught) {
                                saveButton.setEnabled(true);
                                statusLabel.setText("Error: " + caught.getMessage());
                            }
                            public void onSuccess(User updated) {
                                saveButton.setEnabled(true);
                                // keep the local user object in sync
                                currentUser.setBio(bio);
                                currentUser.setSkillTags(tags);
                                statusLabel.setText("Profile saved!");
                            }
                        });
            }
        });

        // --- Navigation back to home ---
        backButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new HomeGui(currentUser).show();
            }
        });
    }

    // "Java, Guitar" -> ["Java", "Guitar"], ignoring empty entries
    private List<String> parseTags(String raw) {
        List<String> tags = new ArrayList<String>();
        if (raw != null) {
            for (String tag : raw.split(",")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    tags.add(trimmed);
                }
            }
        }
        return tags;
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(tags.get(i));
        }
        return sb.toString();
    }
}

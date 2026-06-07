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
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class AnnouncementFormGui {

    private final String currentUsername;
    private final AnnouncementServiceAsync announcementService =
            GWT.create(AnnouncementService.class);

    public AnnouncementFormGui(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Create announcement</h1>");
        final TextBox idField = textField("Announcement ID");
        final TextBox offeredSkillField = textField("Offered skill");
        final TextBox requestedSkillField = textField("Requested skill");
        final TextArea descriptionField = new TextArea();
        descriptionField.getElement().setPropertyString("placeholder", "Description");
        descriptionField.setVisibleLines(5);
        final TextBox availabilityField = textField("Availability");
        final Button createButton = new Button("Create announcement");
        final Button backButton = new Button("Back to marketplace");
        final Label errorLabel = new Label();
        errorLabel.addStyleName("serverResponseLabelError");

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        mainPanel.add(title);
        mainPanel.add(new Label("Announcement ID"));
        mainPanel.add(idField);
        mainPanel.add(new Label("Offered skill"));
        mainPanel.add(offeredSkillField);
        mainPanel.add(new Label("Requested skill"));
        mainPanel.add(requestedSkillField);
        mainPanel.add(new Label("Description"));
        mainPanel.add(descriptionField);
        mainPanel.add(new Label("Availability"));
        mainPanel.add(availabilityField);
        mainPanel.add(createButton);
        mainPanel.add(errorLabel);
        mainPanel.add(backButton);

        RootPanel.get().add(mainPanel);
        idField.setFocus(true);

        createButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                errorLabel.setText("");

                String id = idField.getText();
                String offeredSkill = offeredSkillField.getText();
                String requestedSkill = requestedSkillField.getText();

                if (isBlank(id) || isBlank(offeredSkill) || isBlank(requestedSkill)) {
                    errorLabel.setText(
                            "Announcement ID, offered skill and requested skill are required.");
                    return;
                }

                Announcement announcement = new Announcement(
                        id.trim(),
                        currentUsername,
                        offeredSkill.trim(),
                        requestedSkill.trim(),
                        descriptionField.getText().trim(),
                        availabilityField.getText().trim(),
                        true);

                createButton.setEnabled(false);
                announcementService.createAnnouncement(
                        announcement,
                        new AsyncCallback<Announcement>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                createButton.setEnabled(true);
                                errorLabel.setText(
                                        errorMessage("Unable to create announcement", caught));
                            }

                            @Override
                            public void onSuccess(Announcement createdAnnouncement) {
                                Window.alert("Announcement created.");
                                new MarketplaceGui(currentUsername).show();
                            }
                        });
            }
        });

        backButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new MarketplaceGui(currentUsername).show();
            }
        });
    }

    private TextBox textField(String placeholder) {
        TextBox field = new TextBox();
        field.getElement().setPropertyString("placeholder", placeholder);
        return field;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String errorMessage(String fallback, Throwable caught) {
        if (caught == null || caught.getMessage() == null
                || caught.getMessage().trim().isEmpty()) {
            return fallback + ".";
        }
        return fallback + ": " + caught.getMessage();
    }
}

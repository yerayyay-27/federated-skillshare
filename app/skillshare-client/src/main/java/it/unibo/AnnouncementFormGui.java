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

    private final User currentUser;
    private final Announcement announcementToEdit;
    private final AnnouncementServiceAsync announcementService =
            GWT.create(AnnouncementService.class);

    public AnnouncementFormGui(User currentUser) {
        this(currentUser, null);
    }

    public AnnouncementFormGui(
            User currentUser,
            Announcement announcementToEdit) {
        this.currentUser = currentUser;
        this.announcementToEdit = announcementToEdit;
    }

    public void show() {
        RootPanel.get().clear();

        final boolean editing = announcementToEdit != null;
        HTML title = new HTML(editing
                ? "<h1>Skillshare - Edit announcement</h1>"
                : "<h1>Skillshare - Create announcement</h1>");
        final TextBox offeredSkillField = textField("Offered skill");
        final TextBox requestedSkillField = textField("Requested skill");
        final TextArea descriptionField = new TextArea();
        descriptionField.getElement().setPropertyString("placeholder", "Description");
        descriptionField.setVisibleLines(5);
        final TextBox availabilityField = textField("Availability");
        final Button submitButton = new Button(
                editing ? "Save changes" : "Create announcement");
        final Button backButton = new Button("Back to marketplace");
        final Label errorLabel = new Label();
        errorLabel.addStyleName("serverResponseLabelError");

        if (editing) {
            offeredSkillField.setText(announcementToEdit.getOfferedSkill());
            requestedSkillField.setText(announcementToEdit.getRequestedSkill());
            descriptionField.setText(announcementToEdit.getDescription());
            availabilityField.setText(announcementToEdit.getAvailability());
        }

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        mainPanel.add(title);
        mainPanel.add(new Label("Offered skill"));
        mainPanel.add(offeredSkillField);
        mainPanel.add(new Label("Requested skill"));
        mainPanel.add(requestedSkillField);
        mainPanel.add(new Label("Description"));
        mainPanel.add(descriptionField);
        mainPanel.add(new Label("Availability"));
        mainPanel.add(availabilityField);
        mainPanel.add(submitButton);
        mainPanel.add(errorLabel);
        mainPanel.add(backButton);

        RootPanel.get().add(mainPanel);
        offeredSkillField.setFocus(true);

        submitButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                errorLabel.setText("");

                String offeredSkill = offeredSkillField.getText();
                String requestedSkill = requestedSkillField.getText();

                if (isBlank(offeredSkill) || isBlank(requestedSkill)) {
                    errorLabel.setText(
                            "Offered skill and requested skill are required.");
                    return;
                }

                Announcement announcement = new Announcement(
                        editing ? announcementToEdit.getId() : null,
                        currentUser.getUsername(),
                        offeredSkill.trim(),
                        requestedSkill.trim(),
                        descriptionField.getText().trim(),
                        availabilityField.getText().trim(),
                        editing ? announcementToEdit.isActive() : true);

                submitButton.setEnabled(false);
                if (editing) {
                    updateAnnouncement(announcement, submitButton, errorLabel);
                } else {
                    createAnnouncement(announcement, submitButton, errorLabel);
                }
            }
        });

        backButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new MarketplaceGui(currentUser).show();
            }
        });
    }

    private void createAnnouncement(
            Announcement announcement,
            final Button submitButton,
            final Label errorLabel) {
        announcementService.createAnnouncement(
                announcement,
                new AsyncCallback<Announcement>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        submitButton.setEnabled(true);
                        errorLabel.setText(
                                errorMessage("Unable to create announcement", caught));
                    }

                    @Override
                    public void onSuccess(Announcement createdAnnouncement) {
                        Window.alert(
                                "Announcement created with ID "
                                        + createdAnnouncement.getId()
                                        + ".");
                        new MarketplaceGui(currentUser).show();
                    }
                });
    }

    private void updateAnnouncement(
            Announcement announcement,
            final Button submitButton,
            final Label errorLabel) {
        announcementService.updateAnnouncement(
                currentUser.getUsername(),
                announcement,
                new AsyncCallback<Announcement>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        submitButton.setEnabled(true);
                        errorLabel.setText(
                                errorMessage("Unable to update announcement", caught));
                    }

                    @Override
                    public void onSuccess(Announcement updatedAnnouncement) {
                        Window.alert("Announcement updated.");
                        new MarketplaceGui(currentUser).show();
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

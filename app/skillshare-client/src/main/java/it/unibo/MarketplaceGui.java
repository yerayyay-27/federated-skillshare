package it.unibo;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MarketplaceGui {

    private final User currentUser;
    private final AnnouncementServiceAsync announcementService =
            GWT.create(AnnouncementService.class);
    private final ExchangeServiceAsync exchangeService =
            GWT.create(ExchangeService.class);

    private VerticalPanel announcementListPanel;
    private Label statusLabel;

    public MarketplaceGui(User currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Marketplace</h1>");
        final TextBox searchField = new TextBox();
        searchField.getElement().setPropertyString(
                "placeholder",
                "Search skills or descriptions");

        final Button searchButton = new Button("Search");
        final Button createButton = new Button("Create announcement");
        final Button backButton = new Button("Back to home");
        statusLabel = new Label();
        statusLabel.addStyleName("serverResponseLabelError");
        announcementListPanel = new VerticalPanel();
        announcementListPanel.setSpacing(12);
        announcementListPanel.setWidth("100%");

        HorizontalPanel searchPanel = new HorizontalPanel();
        searchPanel.setSpacing(8);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(12);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        mainPanel.add(title);
        mainPanel.add(searchPanel);
        mainPanel.add(createButton);
        mainPanel.add(backButton);
        mainPanel.add(statusLabel);
        mainPanel.add(announcementListPanel);

        RootPanel.get().add(mainPanel);

        class SearchHandler implements ClickHandler, KeyUpHandler {
            @Override
            public void onClick(ClickEvent event) {
                search(searchField.getText());
            }

            @Override
            public void onKeyUp(KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    search(searchField.getText());
                }
            }
        }

        SearchHandler searchHandler = new SearchHandler();
        searchButton.addClickHandler(searchHandler);
        searchField.addKeyUpHandler(searchHandler);

        createButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new AnnouncementFormGui(currentUser).show();
            }
        });

        backButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new HomeGui(currentUser).show();
            }
        });

        loadActiveAnnouncements();
    }

    private void search(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadActiveAnnouncements();
            return;
        }

        showLoadingMessage("Searching announcements...");
        announcementService.searchActiveAnnouncements(
                query.trim(),
                announcementCallback("Unable to search announcements"));
    }

    private void loadActiveAnnouncements() {
        showLoadingMessage("Loading announcements...");
        announcementService.getActiveAnnouncements(
                announcementCallback("Unable to load announcements"));
    }

    private AsyncCallback<List<Announcement>> announcementCallback(
            final String failureMessage) {
        return new AsyncCallback<List<Announcement>>() {
            @Override
            public void onFailure(Throwable caught) {
                announcementListPanel.clear();
                statusLabel.setText(errorMessage(failureMessage, caught));
            }

            @Override
            public void onSuccess(List<Announcement> announcements) {
                renderAnnouncements(announcements);
            }
        };
    }

    private void renderAnnouncements(List<Announcement> announcements) {
        announcementListPanel.clear();
        statusLabel.setText("");

        if (announcements == null || announcements.isEmpty()) {
            statusLabel.setText("No active announcements found.");
            return;
        }

        for (final Announcement announcement : announcements) {
            VerticalPanel announcementPanel = new VerticalPanel();
            announcementPanel.setSpacing(4);
            announcementPanel.setWidth("100%");
            announcementPanel.add(new Label(
                    "Offered skill: " + displayValue(announcement.getOfferedSkill())));
            announcementPanel.add(new Label(
                    "Requested skill: " + displayValue(announcement.getRequestedSkill())));
            announcementPanel.add(new Label(
                    "Description: " + displayValue(announcement.getDescription())));
            announcementPanel.add(new Label(
                    "Availability: " + displayValue(announcement.getAvailability())));
            announcementPanel.add(new Label(
                    "Published by: " + ownerIdentity(announcement)));

            if (isOwnedByCurrentUser(announcement)) {
                HorizontalPanel ownerActions = new HorizontalPanel();
                ownerActions.setSpacing(6);

                Button editButton = new Button("Edit");
                editButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        new AnnouncementFormGui(currentUser, announcement).show();
                    }
                });

                Button deleteButton = new Button("Delete");
                deleteButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        deleteAnnouncement(announcement.getId());
                    }
                });

                ownerActions.add(editButton);
                ownerActions.add(deleteButton);
                announcementPanel.add(ownerActions);
            } else {
                Button requestButton = new Button("Request exchange");
                requestButton.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        requestExchange(announcement.getId());
                    }
                });
                announcementPanel.add(requestButton);
            }

            announcementListPanel.add(announcementPanel);
        }
    }

    private boolean isOwnedByCurrentUser(Announcement announcement) {
        if (currentUser == null || currentUser.getUsername() == null) {
            return false;
        }
        if (!currentUser.getUsername().equals(announcement.getOwnerUsername())) {
            return false;
        }
        // Only local announcements are editable: the origin instance must
        // match the user's own instance, so a remote "alice@inst-b" is not
        // editable by a local "alice@inst-a".
        String mine = currentUser.getInstance();
        String origin = announcement.getOriginInstance();
        if (mine == null || origin == null) {
            return true; // backwards-compatible with pre-federation data
        }
        return mine.equals(origin);
    }

    // Renders the owner as user@instance (federated identity).
    private String ownerIdentity(Announcement announcement) {
        String owner = announcement.getOwnerUsername();
        if (owner == null || owner.trim().isEmpty()) {
            return "Unknown";
        }
        String origin = announcement.getOriginInstance();
        if (origin == null || origin.trim().isEmpty()) {
            return owner;
        }
        return owner + "@" + origin;
    }

    private void deleteAnnouncement(String announcementId) {
        statusLabel.setText("Deleting announcement...");
        announcementService.deleteAnnouncement(
                announcementId,
                currentUser.getUsername(),
                new AsyncCallback<Boolean>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        statusLabel.setText(
                                errorMessage("Unable to delete announcement", caught));
                    }

                    @Override
                    public void onSuccess(Boolean deleted) {
                        if (Boolean.TRUE.equals(deleted)) {
                            Window.alert("Announcement deleted.");
                            loadActiveAnnouncements();
                        } else {
                            statusLabel.setText(
                                    "The announcement could not be deleted.");
                        }
                    }
                });
    }

    private void requestExchange(String announcementId) {
        String message = Window.prompt("Optional message for the owner:", "");
        statusLabel.setText("Sending request...");
        exchangeService.createRequest(announcementId, currentUser.getUsername(), message,
                new AsyncCallback<ExchangeRequest>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        statusLabel.setText(errorMessage("Unable to send request", caught));
                    }

                    @Override
                    public void onSuccess(ExchangeRequest request) {
                        Window.alert("Request sent.");
                        statusLabel.setText("");
                    }
                });
    }

    private void showLoadingMessage(String message) {
        announcementListPanel.clear();
        statusLabel.setText(message);
    }

    private String displayValue(String value) {
        return value == null || value.trim().isEmpty() ? "Not provided" : value;
    }

    private String errorMessage(String fallback, Throwable caught) {
        if (caught == null || caught.getMessage() == null
                || caught.getMessage().trim().isEmpty()) {
            return fallback + ".";
        }
        return fallback + ": " + caught.getMessage();
    }
}
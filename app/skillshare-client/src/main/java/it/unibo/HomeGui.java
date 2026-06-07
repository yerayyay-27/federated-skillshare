package it.unibo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class HomeGui {

    private final User currentUser;

    public HomeGui(User currentUser) {
        this.currentUser = currentUser;
    }

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Home</h1>");
        HTML welcome = new HTML("<h2>Welcome, " + currentUser.getUsername() + "!</h2>");
        HTML info = new HTML("<p>You are signed in as <b>" + currentUser.getEmail() + "</b></p>");

        final Button marketplaceButton = new Button("Open marketplace");
        final Button profileButton = new Button("My profile");
        final Button logoutButton = new Button("Sign out");

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        mainPanel.add(title);
        mainPanel.add(welcome);
        mainPanel.add(info);
        mainPanel.add(marketplaceButton);
        mainPanel.add(profileButton);
        mainPanel.add(logoutButton);

        RootPanel.get().add(mainPanel);

        // --- Navigate to the marketplace ---
        marketplaceButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new MarketplaceGui(currentUser.getUsername()).show();
            }
        });

        // --- Navigate to the profile screen ---
        profileButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new ProfileGui(currentUser).show();
            }
        });

        // --- Logout: go back to the login screen ---
        logoutButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new LoginGui().show();
            }
        });
    }
}

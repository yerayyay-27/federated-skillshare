package it.unibo;

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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class RegisterGui {

    private final AuthServiceAsync authService = GWT.create(AuthService.class);

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Sign up</h1>");

        final TextBox usernameField = new TextBox();
        usernameField.getElement().setPropertyString("placeholder", "Username");

        final TextBox emailField = new TextBox();
        emailField.getElement().setPropertyString("placeholder", "Email");

        final PasswordTextBox passwordField = new PasswordTextBox();
        passwordField.getElement().setPropertyString("placeholder", "Password (min. 4 characters)");

        final Button registerButton = new Button("Create account");
        final Button backToLoginButton = new Button("Already have an account? Sign in");
        final Label errorLabel = new Label();
        errorLabel.addStyleName("serverResponseLabelError");

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        mainPanel.add(title);
        mainPanel.add(new HTML("<b>Fill in your details:</b>"));
        mainPanel.add(usernameField);
        mainPanel.add(emailField);
        mainPanel.add(passwordField);
        mainPanel.add(registerButton);
        mainPanel.add(errorLabel);
        mainPanel.add(backToLoginButton);

        RootPanel.get().add(mainPanel);
        usernameField.setFocus(true);

        // --- Registration handler ---
        class RegisterHandler implements ClickHandler, KeyUpHandler {
            public void onClick(ClickEvent event) {
                doRegister();
            }
            public void onKeyUp(KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    doRegister();
                }
            }
            private void doRegister() {
                errorLabel.setText("");
                String username = usernameField.getText();
                String email = emailField.getText();
                String password = passwordField.getText();

                if (username.trim().isEmpty() || email.trim().isEmpty() || password.isEmpty()) {
                    errorLabel.setText("Please fill in all fields");
                    return;
                }

                registerButton.setEnabled(false);

                authService.register(username, email, password, new AsyncCallback<User>() {
                    public void onFailure(Throwable caught) {
                        registerButton.setEnabled(true);
                        errorLabel.setText(caught.getMessage());
                    }
                    public void onSuccess(User user) {
                        // Registration successful: go back to the login screen
                        Window.alert("Account created for " + user.getUsername()
                                + "! Please sign in.");
                        new LoginGui().show();
                    }
                });
            }
        }

        RegisterHandler handler = new RegisterHandler();
        registerButton.addClickHandler(handler);
        passwordField.addKeyUpHandler(handler);

        // --- Navigation back to the login screen ---
        backToLoginButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new LoginGui().show();
            }
        });
    }
}
package it.unibo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LoginGui {

    private final AuthServiceAsync authService = GWT.create(AuthService.class);

    public void show() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Login</h1>");

        final TextBox emailField = new TextBox();
        emailField.getElement().setPropertyString("placeholder", "Email");

        final PasswordTextBox passwordField = new PasswordTextBox();
        passwordField.getElement().setPropertyString("placeholder", "Password");

        final Button loginButton = new Button("Sign in");
        final Button goToRegisterButton = new Button("Don't have an account? Sign up");
        final Label errorLabel = new Label();
        errorLabel.addStyleName("serverResponseLabelError");

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        mainPanel.add(title);
        mainPanel.add(new HTML("<b>Enter your credentials:</b>"));
        mainPanel.add(emailField);
        mainPanel.add(passwordField);
        mainPanel.add(loginButton);
        mainPanel.add(errorLabel);
        mainPanel.add(goToRegisterButton);

        RootPanel.get().add(mainPanel);
        emailField.setFocus(true);

        // --- Login handler ---
        class LoginHandler implements ClickHandler, KeyUpHandler {
            public void onClick(ClickEvent event) {
                doLogin();
            }
            public void onKeyUp(KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    doLogin();
                }
            }
            private void doLogin() {
                errorLabel.setText("");
                String email = emailField.getText();
                String password = passwordField.getText();

                if (email.trim().isEmpty() || password.isEmpty()) {
                    errorLabel.setText("Please fill in email and password");
                    return;
                }

                loginButton.setEnabled(false);

                authService.login(email, password, new AsyncCallback<LoginResult>() {
                    public void onFailure(Throwable caught) {
                        // Only genuine transport/server faults reach here now.
                        loginButton.setEnabled(true);
                        errorLabel.setText("Could not reach the server. Please try again.");
                    }
                    public void onSuccess(LoginResult result) {
                        loginButton.setEnabled(true);
                        if (result.isSuccess()) {
                            // Login successful: move to the home screen.
                            new HomeGui(result.getUser()).show();
                        } else {
                            // Expected failure (unknown email / wrong password):
                            // show the reason like the review screen does.
                            errorLabel.setText(result.getErrorReason());
                        }
                    }
                });
            }
        }

        LoginHandler handler = new LoginHandler();
        loginButton.addClickHandler(handler);
        passwordField.addKeyUpHandler(handler);

        // --- Navigation to the registration screen ---
        goToRegisterButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                new RegisterGui().show();
            }
        });
    }
}
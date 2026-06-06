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

public class LoginGui {

    private final AuthServiceAsync authService = GWT.create(AuthService.class);

    public void mostra() {
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Skillshare - Accedi</h1>");

        final TextBox emailField = new TextBox();
        emailField.getElement().setPropertyString("placeholder", "Email");

        final PasswordTextBox passwordField = new PasswordTextBox();
        passwordField.getElement().setPropertyString("placeholder", "Password");

        final Button loginButton = new Button("Entra");
        final Button vaiRegistrazioneButton = new Button("Non hai un account? Registrati");
        final Label errorLabel = new Label();
        errorLabel.addStyleName("serverResponseLabelError");

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        mainPanel.add(title);
        mainPanel.add(new HTML("<b>Inserisci le tue credenziali:</b>"));
        mainPanel.add(emailField);
        mainPanel.add(passwordField);
        mainPanel.add(loginButton);
        mainPanel.add(errorLabel);
        mainPanel.add(vaiRegistrazioneButton);

        RootPanel.get().add(mainPanel);
        emailField.setFocus(true);

        // --- Handler del login ---
        class LoginHandler implements ClickHandler, KeyUpHandler {
            public void onClick(ClickEvent event) {
                eseguiLogin();
            }
            public void onKeyUp(KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    eseguiLogin();
                }
            }
            private void eseguiLogin() {
                errorLabel.setText("");
                String email = emailField.getText();
                String password = passwordField.getText();

                if (email.trim().isEmpty() || password.isEmpty()) {
                    errorLabel.setText("Compila email e password");
                    return;
                }

                loginButton.setEnabled(false);

                authService.login(email, password, new AsyncCallback<Utente>() {
                    public void onFailure(Throwable caught) {
                        loginButton.setEnabled(true);
                        errorLabel.setText(caught.getMessage());
                    }
                    public void onSuccess(Utente utente) {
                        loginButton.setEnabled(true);
                        // Login riuscito: qui si passa alla schermata successiva.
                        // Quando avrai creato il marketplace, sostituisci con:
                        // new MarketplaceGui(utente).mostra();
                        Window.alert("Benvenuto " + utente.getUsername() + "!");
                    }
                });
            }
        }

        LoginHandler handler = new LoginHandler();
        loginButton.addClickHandler(handler);
        passwordField.addKeyUpHandler(handler);

        // --- Navigazione verso la registrazione ---
        vaiRegistrazioneButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                // new RegistrazioneGui().mostra();   // prossimo passo
            }
        });
    }
}
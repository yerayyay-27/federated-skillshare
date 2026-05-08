package it.unibo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class GreetingGui {

    private static final String SERVER_ERROR = "An error occurred while "
            + "attempting to contact the server. Please check your network "
            + "connection and try again.";

    private final GreetingServiceAsync greetingService = GWT.create(GreetingService.class);

    public void mostra() {
        // Pulisce tutto il contenuto del body
        RootPanel.get().clear();

        // Inizializzazione Widget
        HTML title = new HTML("<h1>Web Application Starter Project</h1>");
        final Button sendButton = new Button("Send");
        final TextBox nameField = new TextBox();
        final Label errorLabel = new Label();
        final Button ricettaButton = new Button("Vai a ricetta");
        
        nameField.setText("GWT User");
        sendButton.addStyleName("sendButton");


        // Creazione del Main Panel (VerticalPanel)
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10); // Opzionale: aggiunge un po' di spazio tra i widget
        
        // Impostiamo la larghezza al 100% per permettere l'allineamento interno
        mainPanel.setWidth("100%");

        // Allineamento orizzontale al centro per tutti i widget aggiunti dopo questa riga
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        
        mainPanel.add(title);
        mainPanel.add(new HTML("<b>Please enter your name:</b>"));
        mainPanel.add(nameField);
        mainPanel.add(sendButton);
        mainPanel.add(errorLabel);
        mainPanel.add(ricettaButton);

        // Aggiunta al RootPanel
        RootPanel.get().add(mainPanel);

        // Focus
        nameField.setFocus(true);
        nameField.selectAll();

        // --- Logica DialogBox ---
        final DialogBox dialogBox = new DialogBox();
        dialogBox.setText("Remote Procedure Call");
        dialogBox.setAnimationEnabled(true);
        final Button closeButton = new Button("Close");
        closeButton.getElement().setId("closeButton");
        final Label textToServerLabel = new Label();
        final HTML serverResponseLabel = new HTML();
        
        VerticalPanel dialogVPanel = new VerticalPanel();
        dialogVPanel.addStyleName("dialogVPanel");
        dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
        dialogVPanel.add(textToServerLabel);
        dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
        dialogVPanel.add(serverResponseLabel);
        dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
        dialogVPanel.add(closeButton);
        dialogBox.setWidget(dialogVPanel);

        closeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                dialogBox.hide();
                sendButton.setEnabled(true);
                sendButton.setFocus(true);
            }
        });

        // --- Logica Handler ---
        class MyHandler implements ClickHandler, KeyUpHandler {
            public void onClick(ClickEvent event) {
                sendNameToServer();
            }

            public void onKeyUp(KeyUpEvent event) {
                if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
                    sendNameToServer();
                }
            }

            private void sendNameToServer() {
                errorLabel.setText("");
                String textToServer = nameField.getText();
                if (!FieldVerifier.isValidName(textToServer)) {
                    errorLabel.setText("Please enter at least four characters");
                    return;
                }

                sendButton.setEnabled(false);
                textToServerLabel.setText(textToServer);
                serverResponseLabel.setText("");
                
                greetingService.greetServer(textToServer, new AsyncCallback<GreetingResponse>() {
                    public void onFailure(Throwable caught) {
                        dialogBox.setText("Remote Procedure Call - Failure");
                        serverResponseLabel.addStyleName("serverResponseLabelError");
                        serverResponseLabel.setHTML(SERVER_ERROR);
                        dialogBox.center();
                        closeButton.setFocus(true);
                    }

                    public void onSuccess(GreetingResponse result) {
                        dialogBox.setText("Remote Procedure Call");
                        serverResponseLabel.removeStyleName("serverResponseLabelError");
                        serverResponseLabel.setHTML(new SafeHtmlBuilder()
                                .appendEscaped(result.getGreeting())
                                .appendHtmlConstant("<br><br>I am running ")
                                .appendEscaped(result.getServerInfo())
                                .appendHtmlConstant(".<br><br>It looks like you are using:<br>")
                                .appendEscaped(result.getUserAgent())
                                .toSafeHtml());
                        dialogBox.center();
                        closeButton.setFocus(true);
                    }
                });
            }
        }

        MyHandler handler = new MyHandler();
        sendButton.addClickHandler(handler);
        nameField.addKeyUpHandler(handler);

        // --- Logica Cambio Interfaccia ---
        ricettaButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                // Carica la nuova classe
                new RicettaGui().mostra();
            }
        });
    }
}
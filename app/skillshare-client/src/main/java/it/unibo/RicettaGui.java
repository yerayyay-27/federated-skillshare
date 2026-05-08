package it.unibo;

import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HTML;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;

public class RicettaGui {

    private final IngredientiServiceAsync ingredientiService = GWT.create(IngredientiService.class);

    public void mostra(){
        // Pulisce tutto il contenuto del body
        RootPanel.get().clear();

        HTML title = new HTML("<h1>Esempio Ricetta</h1>");
        final TextBox ingredienteField = new TextBox();
        final TextBox grammiField = new TextBox();
        final Button aggiungiButton = new Button("Aggiungi ingrediente");
        final Button mostraButton = new Button("Mostra ingredienti");
        final Label messageLabel = new Label();

        // Creazione del Main Panel (VerticalPanel)
        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setSpacing(10);
        mainPanel.setWidth("100%");
        mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);



        mainPanel.add(title);
        mainPanel.add(new HTML("<b>Inserisci ingrediente:</b>"));
        mainPanel.add(ingredienteField);
        mainPanel.add(new HTML("<b>Inserisci grammi:</b>"));
        mainPanel.add(grammiField);
        mainPanel.add(aggiungiButton);
        mainPanel.add(new HTML("<br>"));
        mainPanel.add(mostraButton);
        mainPanel.add(messageLabel);

        RootPanel.get().add(mainPanel);

        // Creazione del Panel che conterrà gli ingredienti (VerticalPanel)
        TextArea ricettaTextBox = new TextArea();
        mainPanel.add(ricettaTextBox);

        aggiungiButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                messageLabel.setText("Aggiungendo l'ingrediente...");

                ingredientiService.aggiungiIngrediente(
                    ingredienteField.getText(),
                    Integer.parseInt(grammiField.getText()),
                    new AsyncCallback<Boolean>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            /// ...
                            messageLabel.setText(caught.toString());
                        }

                        @Override
                        public void onSuccess(Boolean response) {
                            if(response){
                                messageLabel.setText("Ingrediente aggiunto");
                            }else{
                                messageLabel.setText("Errore durante l'aggiunta dell'ingrediente");
                            }
                        }
                });


                
            }
        });

        mostraButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                messageLabel.setText("Aspettando gli ingredienti...");
                
                ingredientiService.prendiIngredienti(
                    new AsyncCallback<IngredientiResponse>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            /// ...
                            messageLabel.setText(caught.toString());
                        }

                        @Override
                        public void onSuccess(IngredientiResponse response) {
                            if(response == null || response.status != 200){
                                messageLabel.setText("Errore durante la richiesta degli ingredienti");
                                return;
                            }
                            messageLabel.setText("Ingredienti ricevuti!");

                            mostraIngredienti(ricettaTextBox, response.ingredienti, response.grammi);                            
                        }
                });


                
            }
        });



    }

    private void mostraIngredienti(TextArea textBox, List<String> ingredienti, List<Integer> grammi){
        textBox.setText("");

        String ricetta = "";

        for(int i = 0; i<ingredienti.size() && i<grammi.size(); i++){
            ricetta += ingredienti.get(i) + " " + grammi.get(i).toString() + " grammi\n";
        }

        textBox.setText(ricetta);
    }
    
}

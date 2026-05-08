package it.unibo;

import java.util.List;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class IngredientiServiceImpl extends RemoteServiceServlet implements
        IngredientiService{

        @Override
        public Boolean aggiungiIngrediente(String ingrediente, Integer grammi) throws IllegalArgumentException {
            //System.out.println("Aggiungendo un ingrediente...");
            // Aggiungere eventuali controlli sugli input

            return DatabaseIngredientiExample.aggiungiIngrediente(ingrediente, grammi);
        }

        @Override
        public IngredientiResponse prendiIngredienti() throws IllegalArgumentException {

            IngredientiResponse response = new IngredientiResponse();
            response.ingredienti = DatabaseIngredientiExample.listaIngredienti();
            response.grammi = DatabaseIngredientiExample.listaGrammi();
            response.status = 200; // Status 200 : OK


            return response;
        }
    
}

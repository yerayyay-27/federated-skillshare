package it.unibo;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The async counterpart of <code>IngredientiService</code>.
 */

public interface IngredientiServiceAsync {

    void aggiungiIngrediente(String ingrediente, Integer grammi, AsyncCallback<Boolean> callback);
    void prendiIngredienti(AsyncCallback<IngredientiResponse> callback);
    
}

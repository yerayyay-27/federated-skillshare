package it.unibo;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet;

public class AuthServiceImpl extends RemoteServiceServlet implements AuthService {

    // Archiviazione in memoria (provvisoria, finché non c'è un database)
    // chiave = email, valore = [password, username]
    private final Map<String, String[]> utenti = new HashMap<String, String[]>();

    public AuthServiceImpl() {
        // utente di prova per testare subito il login
        utenti.put("test@unibo.it", new String[] { "1234", "TestUser" });
    }

    @Override
    public Utente login(String email, String password) throws IllegalArgumentException {
        if (email == null || password == null
                || email.trim().isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Email e password sono obbligatori");
        }
        String[] datiUtente = utenti.get(email.trim());
        if (datiUtente == null || !datiUtente[0].equals(password)) {
            throw new IllegalArgumentException("Email o password non corretti");
        }
        return new Utente(datiUtente[1], email.trim());
    }

    @Override
    public Utente registra(String username, String email, String password)
            throws IllegalArgumentException {
        if (username == null || username.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || password == null || password.length() < 4) {
            throw new IllegalArgumentException(
                "Dati non validi: la password deve avere almeno 4 caratteri");
        }
        if (utenti.containsKey(email.trim())) {
            throw new IllegalArgumentException("Esiste già un utente con questa email");
        }
        utenti.put(email.trim(), new String[] { password, username.trim() });
        return new Utente(username.trim(), email.trim());
    }
}
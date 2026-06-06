package it.unibo;

import java.io.Serializable;

public class Utente implements Serializable {
    private String username;
    private String email;

    public Utente() { } // necessario per la serializzazione GWT

    public Utente(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
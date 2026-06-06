Qui andranno inseriti tutti gli artefatti come: manuale sviluppatore, manualte utente, diaro del progetto, ecc...

Diario del progetto:
06/06/2026- Today we decided the task of each member, and started deciding the design of the project.

Implemented the user authentication (login) feature as a complete end-to-end RPC slice, covering the "Access & Registration" part of Identity & Profile Management. On the shared module we defined the User data object and the AuthService/AuthServiceAsync interfaces; on the server we added AuthServiceImpl with temporary in-memory user storage and a seeded test account, registered as authServlet (/app/auth) in web.xml; and on the client we built the LoginGui screen and updated App.java to load it as the entry point. During testing the server returned HTTP 503 because the servlet failed to deploy on Jetty 12/Jakarta: the cause was extending the legacy javax-based RemoteServiceServlet, which we fixed by switching to the Jakarta variant (com.google.gwt.user.server.rpc.jakarta.RemoteServiceServlet), establishing the convention that all server-side *Impl classes must use it. Finally, we standardized the new code to English across identifiers, comments, and user-facing messages.


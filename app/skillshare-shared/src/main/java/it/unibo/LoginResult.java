package it.unibo;

import java.io.Serializable;

/**
 * Result of a login attempt. Mirrors the "block reason" pattern used by
 * reviews: instead of throwing on a failed login (which reaches the browser as
 * a generic RPC failure with no usable message), the service returns this
 * object. On success it carries the authenticated user; on failure it carries a
 * human-readable reason the GUI can show directly.
 */
public class LoginResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private User user;           // set on success, null on failure
    private String errorReason;  // set on failure, null on success

    public LoginResult() {
    }

    public static LoginResult success(User user) {
        LoginResult result = new LoginResult();
        result.user = user;
        return result;
    }

    public static LoginResult failure(String reason) {
        LoginResult result = new LoginResult();
        result.errorReason = reason;
        return result;
    }

    public boolean isSuccess() {
        return user != null && errorReason == null;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getErrorReason() { return errorReason; }
    public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
}
package com.example.mborper.breathbetter.login.pojo;

/**
 * Represents a request to log in, containing the email and password
 * provided by the user for authentication purposes.
 *
 * @author Alejandro Rosado
 * @since 2024-10-28
 */
public class LoginRequest {
    private String email;
    private String password;

    /**
     * Constructs a new {@code LoginRequest} with the specified email and password.
     *
     * @param email the email provided by the user.
     * @param password the password provided by the user.
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Gets the email provided in the login request.
     *
     * @return the user's email.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email for the login request.
     *
     * @param email the user's email to set.
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the password provided in the login request.
     *
     * @return the user's password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password for the login request.
     *
     * @param password the user's password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }
}

package com.example.mborper.breathbetter.login.pojo;

/**
 * Represents a response for a login attempt, containing the user details
 * and a message indicating the result of the login operation.
 *
 * @author Alejandro Rosado
 * @since 2024-10-28
 */
public class LoginResponse {
    private User user;
    private String message;

    /**
     * Gets the {@link User} object from the login response.
     *
     * @return the user associated with the login response.
     */
    public User getUser() { return user; }

    /**
     * Sets the {@link User} object for the login response.
     *
     * @param user the user to associate with the login response.
     */
    public void setUser(User user) { this.user = user; }

    /**
     * Gets the message associated with the login response.
     *
     * @return the login response message.
     */
    public String getMessage() { return message; }

    /**
     * Sets the message for the login response.
     *
     * @param message the message to associate with the login response.
     */
    public void setMessage(String message) { this.message = message; }
}

package com.example.mborper.breathbetter.login.pojo;

// LoginResponse.java
public class LoginResponse {
    private String message;
    private User user;
    private String userId;
    private String email;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}

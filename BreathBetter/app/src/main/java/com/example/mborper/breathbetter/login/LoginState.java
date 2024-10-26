package com.example.mborper.breathbetter.login;

import com.example.mborper.breathbetter.login.pojo.LoginResponse;

// LoginState.java
public class LoginState {

    private Status status;
    private LoginResponse data;
    private LoginResponse loginResponse;
    private String userId;
    private String email;
    private String error;

    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }

    // Constructor
    public LoginState(Status status, LoginResponse loginResponse, String error) {
        this.status = status;
        this.loginResponse = loginResponse;
        this.error = error;
    }

    // Métodos estáticos para crear instancias de LoginState
    public static LoginState loading() {
        return new LoginState(Status.LOADING, null, null);
    }

    public static LoginState success(LoginResponse response) {
        return new LoginState(Status.SUCCESS, response, null);
    }

    public static LoginState error(String error) {
        return new LoginState(Status.ERROR, null, error);
    }

    public Status getStatus() {
        return status;
    }

    public LoginResponse getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
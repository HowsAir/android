package com.example.mborper.breathbetter.login;

import android.content.Context;
import android.content.SharedPreferences;

// SessionManager.java
public class SessionManager {
    private static final String PREF_NAME = "LoginPrefs";
    private static final String AUTH_TOKEN = "auth_token";
    private static final String USER_EMAIL = "user_email";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveAuthToken(String token) {
        editor.putString(AUTH_TOKEN, token);
        editor.apply();
    }

    public String getAuthToken() {
        return prefs.getString(AUTH_TOKEN, null);
    }

    public void saveUserEmail(String email) {
        editor.putString(USER_EMAIL, email);
        editor.apply();
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public boolean isLoggedIn() {
        return getAuthToken() != null;
    }
}

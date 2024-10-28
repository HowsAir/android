package com.example.mborper.breathbetter.login;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages user session data for the BreathBetter application,
 * handling storage and retrieval of session information such as the
 * authentication token and node ID.
 *
 * @author Manuel Borregales
 * @since 2024-10-28
 */
public class SessionManager {
    private static final String PREF_NAME = "LoginPrefs";
    private static final String AUTH_TOKEN = "auth_token";
    private static final String NODE_ID = "nodeId"; // Nueva clave para el nodeId

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Context context;

    /**
     * Initializes a new {@code SessionManager} with the specified application context.
     *
     * @param context the application context for accessing shared preferences.
     */
    public SessionManager(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Saves the user's authentication token in shared preferences.
     *
     * @param token the authentication token to save.
     */
    public void saveAuthToken(String token) {
        editor.putString(AUTH_TOKEN, token);
        editor.apply();
    }

    /**
     * Retrieves the user's authentication token from shared preferences.
     *
     * @return the stored authentication token, or {@code null} if not found.
     */
    public String getAuthToken() {
        return prefs.getString(AUTH_TOKEN, null);
    }

    /**
     * Saves the node ID in shared preferences.
     *
     * @param nodeId the node ID to save.
     */
    public void saveNodeId(String nodeId) {
        editor.putString(NODE_ID, nodeId);
        editor.apply();
    }

    /**
     * Retrieves the node ID from shared preferences.
     *
     * @return the stored node ID, or {@code null} if not found.
     */
    public String getNodeId() {
        return prefs.getString(NODE_ID, null);
    }

    /**
     * Clears all session data, including the authentication token and node ID.
     */
    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    /**
     * Checks if a user session exists by verifying the presence of an authentication token.
     *
     * @return {@code true} if the user is logged in, {@code false} otherwise.
     */
    public boolean isLoggedIn() {
        return getAuthToken() != null;
    }
}

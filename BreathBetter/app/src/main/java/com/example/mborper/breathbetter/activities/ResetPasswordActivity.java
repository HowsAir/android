package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.login.SessionManager;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles the functionality for resetting the user's password within the BreathBetter app.
 * This activity allows users to enter a new password and confirm it. If the passwords match,
 * a request is sent to the server to update the password. Upon successful reset, the user
 * is redirected to the login screen.
 *
 * This class utilizes the Retrofit library to interact with the backend server.
 *
 * @author Manuel
 * @since 2024-11-13
 * last updated: 2024-11-21
 */
public class ResetPasswordActivity extends AppCompatActivity {
    private EditText etNewPassFP, etVerifyPassFP;
    private Button btnChangePassFP;

    private ApiService apiService;
    private SessionManager sessionManager;

    /**
     * Initializes the activity, setting up the layout and UI components.
     * Also sets up the Retrofit API service for making network requests.
     *
     * @param savedInstanceState If the activity is being reinitialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        sessionManager = new SessionManager(this);

        etNewPassFP = findViewById(R.id.etNewPassFP);
        etVerifyPassFP = findViewById(R.id.etVerifyPassFP);
        btnChangePassFP = findViewById(R.id.btnChangePassFP);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        btnChangePassFP.setOnClickListener(v -> resetPassword());
    }

    /**
     * Handles the process of resetting the user's password. It validates that the two password fields match,
     * then sends a request to the backend to update the password.
     */
    private void resetPassword() {
        String newPassword = etNewPassFP.getText().toString().trim();
        String verifyPassword = etVerifyPassFP.getText().toString().trim();

        TextView tvMismatchPassword = findViewById(R.id.tvMismatchPassword);

        if (!newPassword.equals(verifyPassword)) {
            tvMismatchPassword.setVisibility(View.VISIBLE);
            return;
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("newPassword", newPassword);

        // Make a network call to the /users/reset-password endpoint
        apiService.resetPassword(requestBody).enqueue(new Callback<Void>() {

            /**
             * Handles the response from the password reset request. If successful, it notifies the user
             * and redirects them to the login screen. If not, it shows an error message.
             *
             * @param call The call to the API that was executed.
             * @param response The response returned by the API call.
             */
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                TextView tvMismatchPassword = findViewById(R.id.tvMismatchPassword);

                if (response.isSuccessful()) {
                    tvMismatchPassword.setVisibility(View.VISIBLE);
                    tvMismatchPassword.setText("Contraseña cambiada correctamente");
                    tvMismatchPassword.setTextColor(getResources().getColor(R.color.black));

                    sessionManager.clearSession();

                    startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                    finish();
                } else {
                    tvMismatchPassword.setVisibility(View.VISIBLE);
                    tvMismatchPassword.setText("Las contraseñas deben coincidir");
                }
            }

            /**
             * Handles network failures during the password reset process.
             * Displays an error message with details about the failure.
             *
             * @param call The call to the API that was attempted.
             * @param t The throwable representing the network error.
             */
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                TextView tvMismatchPassword = findViewById(R.id.tvMismatchPassword);

                tvMismatchPassword.setVisibility(View.VISIBLE);
                tvMismatchPassword.setText("Error de conexión");

            }
        });
    }
}
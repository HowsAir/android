package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {
    private EditText etNewPassFP, etVerifyPassFP;
    private Button btnChangePassFP;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        // Initialize views

        etNewPassFP = findViewById(R.id.etNewPassFP);
        etVerifyPassFP = findViewById(R.id.etVerifyPassFP);
        btnChangePassFP = findViewById(R.id.btnChangePassFP);

        // Initialize Retrofit API service
        apiService = ApiClient.getClient(this).create(ApiService.class);

        // Set click listener for "Change Password" button
        btnChangePassFP.setOnClickListener(v -> resetPassword());
    }



    private void resetPassword() {
        String newPassword = etNewPassFP.getText().toString().trim();
        String verifyPassword = etVerifyPassFP.getText().toString().trim();

        // Validate the password fields
        if (!newPassword.equals(verifyPassword)) {
            Toast.makeText(ResetPasswordActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a JSON object with the email field
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("newPassword", newPassword);

        // Call the /users/reset-password endpoint
        apiService.resetPassword(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Password reset successful, redirect to the login screen
                    Toast.makeText(ResetPasswordActivity.this, "Password reset successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                    finish();
                } else {
                    // Handle the error case
                    Toast.makeText(ResetPasswordActivity.this, "Failed to reset password", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Handle the network error case
                Toast.makeText(ResetPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
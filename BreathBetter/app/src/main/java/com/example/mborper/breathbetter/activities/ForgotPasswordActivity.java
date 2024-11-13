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

public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText etEmailFP, etVerificationCodeFP, etNewPassFP, etVerifyPassFP;
    private Button btnRequestCodeFP, btnSendCodeFP;

    private ApiService apiService;

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize views
        etEmailFP = findViewById(R.id.etEmailFP);
        btnRequestCodeFP = findViewById(R.id.btnRequestCodeFP);
        etVerificationCodeFP = findViewById(R.id.etVerificationCodeFP);
        btnSendCodeFP = findViewById(R.id.btnSendCodeFP);

        // Initialize Retrofit API service
        apiService = ApiClient.getClient(this).create(ApiService.class);

        // Set click listener for "Request Code" button
        btnRequestCodeFP.setOnClickListener(v -> requestPasswordResetCode());
        // Set click listener for "Send Code" button
        btnSendCodeFP.setOnClickListener(v -> verifyResetCode());
    }

    private void requestPasswordResetCode() {
        email = etEmailFP.getText().toString().trim();

        // Create a JSON object with the email field
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);

        // Call the /auth/forgot-password-code endpoint
        apiService.forgotPasswordCode(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Display a message to the user that the code has been sent
                    Toast.makeText(ForgotPasswordActivity.this, "Email sent if you have an account", Toast.LENGTH_SHORT).show();
                    btnSendCodeFP.setEnabled(true);
                    btnSendCodeFP.setBackgroundColor(getResources().getColor(R.color.primary)); // Assuming you've created a drawable for enabled state
                    btnSendCodeFP.setTextColor(getResources().getColor(R.color.white));
                } else {
                    // Handle the error case
                    Toast.makeText(ForgotPasswordActivity.this, "Failed to request password reset code", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Handle the network error case
                Toast.makeText(ForgotPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void verifyResetCode() {
        String code = etVerificationCodeFP.getText().toString().trim();

        // Call the /auth/forgot-password-token endpoint with email and code as query parameters
        apiService.forgotPasswordToken(email, code).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // The reset code is valid, display the password reset form
                    Toast.makeText(ForgotPasswordActivity.this, "Password reset code is valid", Toast.LENGTH_SHORT).show();

                    // Navigate to the password reset activity
                    Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                } else {
                    // Handle the error case
                    Toast.makeText(ForgotPasswordActivity.this, "Invalid password reset code", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Handle the network error case
                Toast.makeText(ForgotPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

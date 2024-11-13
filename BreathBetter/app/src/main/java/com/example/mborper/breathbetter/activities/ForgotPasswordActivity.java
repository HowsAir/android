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

/**
 * Manages the Forgot Password process in the BreathBetter app. This activity handles
 * sending a password reset code to the user's email and verifying the code to allow
 * the user to reset their password. It utilizes the Retrofit API service to interact
 * with the backend server.
 *
 * Users are required to enter their email address to request a password reset code.
 * If successful, they can verify the code and proceed to reset their password.
 *
 * @author Manuel
 * @since 2024-11-13
 */
public class ForgotPasswordActivity extends AppCompatActivity {
    private EditText etEmailFP, etVerificationCodeFP;
    private Button btnRequestCodeFP, btnSendCodeFP;

    private ApiService apiService;

    private String email;

    /**
     * Initializes the ForgotPasswordActivity, setting up the layout and UI components.
     * Also initializes the Retrofit API service for handling requests to the backend server.
     *
     * @param savedInstanceState If the activity is being reinitialized after previously
     *                           being shut down, this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmailFP = findViewById(R.id.etEmailFP);
        btnRequestCodeFP = findViewById(R.id.btnRequestCodeFP);
        etVerificationCodeFP = findViewById(R.id.etVerificationCodeFP);
        btnSendCodeFP = findViewById(R.id.btnSendCodeFP);

        apiService = ApiClient.getClient(this).create(ApiService.class);

        btnRequestCodeFP.setOnClickListener(v -> requestPasswordResetCode());
        btnSendCodeFP.setOnClickListener(v -> verifyResetCode());
    }

    /**
     * Sends a password reset code to the email provided by the user. This method
     * makes a POST request to the /auth/forgot-password-code endpoint.
     * If successful, it enables the "Send Code" button for the user to verify the code.
     */
    private void requestPasswordResetCode() {
        email = etEmailFP.getText().toString().trim();

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("email", email);

        apiService.forgotPasswordCode(requestBody).enqueue(new Callback<Void>() {
            /**
             * Handles the response from the password reset code request. If successful,
             * it notifies the user that the email has been sent and enables the next step.
             *
             * @param call The call to the API that was executed.
             * @param response The response returned by the API call.
             */
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Email sent if you have an account",
                            Toast.LENGTH_SHORT).show();

                    // Enable the "Send Code" button and set its active style
                    btnSendCodeFP.setEnabled(true);
                    btnSendCodeFP.setBackgroundColor(getResources().getColor(R.color.primary));
                    btnSendCodeFP.setTextColor(getResources().getColor(R.color.white));
                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Failed to request password reset code",
                            Toast.LENGTH_SHORT).show();
                }
            }

            /**
             * Handles network failures during the password reset request.
             * Displays an error message to the user.
             *
             * @param call The call to the API that was attempted.
             * @param t The throwable representing the network error.
             */
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ForgotPasswordActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Verifies the password reset code entered by the user. This method makes a GET
     * request to the /auth/forgot-password-token endpoint with the provided code.
     * If successful, the user is redirected to the ResetPasswordActivity.
     */
    private void verifyResetCode() {
        String code = etVerificationCodeFP.getText().toString().trim();

        apiService.forgotPasswordToken(email, code).enqueue(new Callback<Void>() {
            /**
             * Handles the response from the verification code check. If successful,
             * navigates the user to the ResetPasswordActivity.
             *
             * @param call The call to the API that was executed.
             * @param response The response returned by the API call.
             */
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                } else {
                    Toast.makeText(ForgotPasswordActivity.this,
                            "Invalid password reset code",
                            Toast.LENGTH_SHORT).show();
                }
            }

            /**
             * Handles network failures during the verification code check.
             * Displays an error message to the user.
             *
             * @param call The call to the API that was attempted.
             * @param t The throwable representing the network error.
             */
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ForgotPasswordActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}


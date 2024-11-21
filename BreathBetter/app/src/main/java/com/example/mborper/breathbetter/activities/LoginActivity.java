package com.example.mborper.breathbetter.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.login.SessionManager;
import com.example.mborper.breathbetter.login.pojos.LoginRequest;
import com.example.mborper.breathbetter.login.pojos.LoginResponse;

import java.util.List;

import okhttp3.Headers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles user login functionality for the BreathBetter app, including
 * validating user input, initiating the login request to the API, and handling
 * login responses. If login is successful, the user is redirected to the main
 * activity; otherwise, appropriate feedback is displayed.
 *
 * @author Manuel Borregales
 * @since 2024-10-28
 * last edited 2024-11-13
 */
public class LoginActivity extends AppCompatActivity {
    private EditText emailEdit, passwordEdit;
    private TextView tvForgotPass;
    private Button loginButton;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    /**
     * Initializes the activity. Sets up UI components, checks for existing user sessions,
     * and redirects to the main activity if the user is already logged in.
     *
     * @param savedInstanceState If the activity is being reinitialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Initialize UI components
        emailEdit = findViewById(R.id.etEmail);
        passwordEdit = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        tvForgotPass = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        // Set up login button click listener
        loginButton.setOnClickListener(v -> performLogin());
        tvForgotPass.setOnClickListener(v -> onTvForgotPassClicked());


        TextView termsPrivacyTextView = findViewById(R.id.txtbTermsPrivacy);
        String fullText = getString(R.string.PrivacyText);

        // Apply the custom colors
        applyColoredText(termsPrivacyTextView, fullText, "Términos de servicio", "Política de Privacidad");

        // Set up terms and privacy text view click listener
        termsPrivacyTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, TermsAndPrivacyActivity.class));
            }
        });

    }

    /**
     * Validates the user's input, then initiates a login request through the API.
     * If fields are empty, it prompts the user to fill in all fields.
     * While the request is processed, the progress bar is shown, and the login button is disabled.
     */
    private void performLogin() {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        TextView tvError = findViewById(R.id.tvError);


        if (email.isEmpty() || password.isEmpty()) {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("Por favor, completa todos los campos");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        LoginRequest loginRequest = new LoginRequest(email, password);
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            /**
             * Handles a successful login response from the API. If authentication is successful,
             * it saves the user's auth token and email, then redirects to the main activity.
             * If authentication fails, it shows an error message.
             *
             * @param call The call to the API that was executed.
             * @param response The response returned by the API call.
             */
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                TextView tvError = findViewById(R.id.tvError);
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);

                if (response.isSuccessful()) {
                    // Get auth token from the response cookies
                    String authToken = null;
                    Headers headers = response.headers();
                    List<String> cookies = headers.values("Set-Cookie");
                    for (String cookie : cookies) {
                        if (cookie.startsWith("auth_token=")) {
                            authToken = cookie.split(";")[0].substring("auth_token=".length());
                            break;
                        }
                    }

                    if (authToken != null) {
                        sessionManager.saveAuthToken(authToken);

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        tvError.setVisibility(View.VISIBLE);
                        tvError.setText("Autenticación fallida");
                    }
                } else {
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText("Error de autenticación o credenciales inválidas");
                }
            }

            /**
             * Handles a failure in the login process due to network or other issues.
             * Displays an error message to the user with details about the failure.
             *
             * @param call The call to the API that was attempted.
             * @param t The throwable representing the error that occurred.
             */
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                TextView tvError = findViewById(R.id.tvError);
                loginButton.setEnabled(true);

                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Error de conexión");
            }
        });
    }

    /**
     * Opens the forgot password activity
     */
    private void onTvForgotPassClicked() {
        startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
    }

    /**
     * This method applies custom colors to specific parts of the provided text.
     * It changes the color of "Términos de servicio" and "Política de Privacidad"
     * within the text and applies the specified color to each part.
     *
     * @param textView The TextView to which the formatted text will be set.
     * @param fullText The full text containing the phrases to be colored.
     * @param terms The phrase that should be colored (e.g., "Términos de servicio").
     * @param privacy The phrase that should be colored (e.g., "Política de Privacidad").
     */
    private void applyColoredText(TextView textView, String fullText, String terms, String privacy) {
        // Find the start and end indices for the "terms" and "privacy" phrases
        int termsStart = fullText.indexOf(terms);
        int termsEnd = termsStart + terms.length();
        int privacyStart = fullText.indexOf(privacy);
        int privacyEnd = privacyStart + privacy.length();

        // Create a SpannableString to modify parts of the text
        SpannableString spannableString = new SpannableString(fullText);

        // Get the color for the terms and privacy sections
        int termsColor = ContextCompat.getColor(this, R.color.primary);
        int privacyColor = ContextCompat.getColor(this, R.color.primary);

        // Apply the color to the specific sections of the text
        spannableString.setSpan(new ForegroundColorSpan(termsColor), termsStart, termsEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(privacyColor), privacyStart, privacyEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the modified text to the TextView
        textView.setText(spannableString);
    }
}

package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.login.SessionManager;
import com.example.mborper.breathbetter.login.pojo.LoginRequest;
import com.example.mborper.breathbetter.login.pojo.LoginResponse;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        //Si esta loggeado pero no ha vinculado el sensor, , lo mandamos al QRExplanationActivity,
        // si ya esta vinculado, lo mandamos a MainActivity
        if (sessionManager.isLoggedIn()) {
            // Redirects to main activity if the user is already logged in

            //Puede darse que ya haya vinculado el nodo o no lo haya vinculado,

            //SUSTITUIR SPRINT 2 POR LLAMADA A ENDPOINT QUE COMPRUEBE SI ESTE USER YA TIENE NODO
            //SI NO TIENE NODO LO MANDAMOS A QR SI YA TIENE OBTENEMOS SU NODEID LO GUARDAMOS Y
            //LO MANDAMOS A MAIN ACTIVITY

            //(eso lo comprobamos llamando a un endpoint
            // de la api)
            if(sessionManager.getNodeId() == null){
                startActivity(new Intent(LoginActivity.this, QRExplanationActivity.class));
            }
            else{
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }


            finish();
        }

        // Initialize UI components
        emailEdit = findViewById(R.id.etEmail);
        passwordEdit = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        tvForgotPass = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressBar);

        // Set up login button click listener
        loginButton.setOnClickListener(v -> performLogin());
        tvForgotPass.setOnClickListener(v -> onTvForgotPassClicked());
    }

    /**
     * Validates the user's input, then initiates a login request through the API.
     * If fields are empty, it prompts the user to fill in all fields.
     * While the request is processed, the progress bar is shown, and the login button is disabled.
     */
    private void performLogin() {
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
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
                        // Save auth token and user email in session manager
                        sessionManager.saveAuthToken(authToken);

                        // Redirects to main activity
                        //Aqui se ejecutaria la misma comprobacion de la api, que hay en oncreate
                        //o parecida
                        startActivity(new Intent(LoginActivity.this, QRExplanationActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
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
                loginButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Opens the forgot password activity
     */
    private void onTvForgotPassClicked() {
        startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
    }
}

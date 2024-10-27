package com.example.mborper.breathbetter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class LoginActivity extends AppCompatActivity {
    private EditText emailEdit, passwordEdit;
    private Button loginButton;
    private ProgressBar progressBar;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            // Navigate to main activity if already logged in
            startActivity(new Intent(LoginActivity.this, QRExplanationActivity.class));
            finish();
        }

        emailEdit = findViewById(R.id.etEmail);
        passwordEdit = findViewById(R.id.etPassword);
        loginButton = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        loginButton.setOnClickListener(v -> performLogin());
    }

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
        ApiService apiService = ApiClient.getClient().create(ApiService.class);

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);

                if (response.isSuccessful()) {
                    // Get auth token from cookie
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
                        // Save auth token and user email
                        sessionManager.saveAuthToken(authToken);
                        sessionManager.saveUserEmail(email);

                        // Navigate to main activity
                        startActivity(new Intent(LoginActivity.this, QRExplanationActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
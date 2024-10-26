package com.example.mborper.breathbetter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mborper.breathbetter.databinding.ActivityLoginBinding;
import com.example.mborper.breathbetter.login.LoginViewModel;
import com.example.mborper.breathbetter.login.SessionManager;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;
    private SessionManager sessionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);


        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        setupObservers();
        setupListeners();
    }

    private void setupObservers() {
        viewModel.getLoginState().observe(this, state -> {
            switch (state.getStatus()) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.btnLogin.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnLogin.setEnabled(true);

                    String userId = state.getUserId();
                    String email = state.getEmail();

                    // Crea la sesión de inicio de sesión aquí
                    sessionManager.createLoginSession(userId, email);

                    // Redirige a MainActivity
                    startActivity(new Intent(this, QRExplanationActivity.class));
                    finish();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnLogin.setEnabled(true);
                    Toast.makeText(this, state.getError(), Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString();
            String password = binding.etPassword.getText().toString();
            if (validateInput(email, password)) {
                viewModel.login(email, password);
            }
        });

        // Opcional: Agregar listener para "Olvidaste tu contraseña"
        binding.tvForgotPassword.setOnClickListener(v -> {
            // Implementar la lógica para recuperar contraseña
        });
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            binding.tilEmail.setError("Email is required");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Invalid email format");
            return false;
        }
        if (password.isEmpty()) {
            binding.tilPassword.setError("Password is required");
            return false;
        }

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
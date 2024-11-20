package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.login.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity responsible for changing the user's password.
 * <p>
 * Allows users to enter their current password, a new password, and confirmation.
 * Validates inputs and sends a request to the server to update the password.
 *
 * @author Alejandro Rosado
 * @since 2024-11-20
 * last edited:
 */
public class ChangePasswordActivity extends AppCompatActivity {
    private static final String LOG_TAG = "CHANGE_PASSWORD_LOG";

    private TextInputEditText etCurrentPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnChangePassword;
    private ApiService apiService;
    private SessionManager sessionManager;

    // Password validation pattern
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialize components
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        findViewById(R.id.btnBack2).setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(this).create(ApiService.class);

        setupTextWatchers();
        setupChangePasswordButton();
    }

    /**
     * Adds TextWatchers to validate user inputs as they type.
     */
    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateFields();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etCurrentPassword.addTextChangedListener(textWatcher);
        etNewPassword.addTextChangedListener(textWatcher);
        etConfirmPassword.addTextChangedListener(textWatcher);
    }

    /**
     * Validates user inputs and updates the button state.
     */
    private void validateFields() {
        String currentPassword = etCurrentPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        boolean isValid = !currentPassword.isEmpty() &&
                !newPassword.isEmpty() &&
                !confirmPassword.isEmpty() &&
                newPassword.equals(confirmPassword) &&
                isValidPassword(newPassword);

        btnChangePassword.setEnabled(isValid);
        btnChangePassword.setBackgroundTintList(
                getColorStateList(isValid ? R.color.primary : R.color.gray)
        );
    }

    /**
     * Checks if a password meets the defined pattern.
     *
     * @param password the password to validate
     * @return true if the password is valid, false otherwise
     */
    private boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Sets up the change password button with click behavior.
     */
    private void setupChangePasswordButton() {
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    /**
     * Sends a request to the server to change the password.
     */
    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("currentPassword", currentPassword);
        requestBody.addProperty("newPassword", newPassword);

        apiService.changePassword(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showToast("Contraseña cambiada exitosamente");
                    logout();
                } else {
                    Log.e(LOG_TAG, "Error al cambiar contraseña: " + response.code());
                    showToast("Error al cambiar la contraseña. Verifica tus datos.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(LOG_TAG, "Fallo de conexión al cambiar contraseña", t);
                showToast("Error de conexión al cambiar la contraseña");
            }
        });
    }

    /**
     * Logs the user out and redirects to the login activity.
     */
    private void logout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Displays a toast message on the screen.
     *
     * @param message the message to display
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
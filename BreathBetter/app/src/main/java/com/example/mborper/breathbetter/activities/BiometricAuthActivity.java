package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.login.SessionManager;

import java.util.concurrent.Executor;


/**
 * Activity that handles biometric authentication at app startup.
 * If the device doesn't support biometric authentication or has no biometric data enrolled,
 * it will automatically proceed to the next activity.
 *
 * @since 2024-11-01
 * last edited: 2024-11-02
 * @author Alejandro Rosado
 */
public class BiometricAuthActivity extends AppCompatActivity {

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_biometric_auth);

        sessionManager = new SessionManager(this);

        // Check if user is logged in
        if (!sessionManager.isLoggedIn()) {
            // If not logged in, go directly to login activity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // User is logged in, proceed with biometric authentication
        if (isBiometricAvailable()) {
            setupBiometricAuth();
            showBiometricPrompt();
        } else {
            // If biometric auth is not available, proceed to LoginActivity
            // which will handle the navigation logic
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    /**
     * Checks if biometric authentication is available and can be used on this device.
     * This includes checking if the device has the necessary hardware and if biometric
     * data is enrolled.
     *
     * @return true if biometric authentication is available, false otherwise
     */
    private boolean isBiometricAvailable() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);

        switch (result) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return true;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                // The device does not have a biometric sensor
                Toast.makeText(this,
                        "Este dispositivo no tiene sensor de huellas",
                        Toast.LENGTH_LONG).show();
                break;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                // Biometric hardware is not currently available
                Toast.makeText(this,
                        "El sensor de huellas no está disponible actualmente",
                        Toast.LENGTH_LONG).show();
                break;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // There are no registered footprints
                Toast.makeText(this,
                        "No hay huellas registradas en el dispositivo",
                        Toast.LENGTH_LONG).show();

                // Optionally, we could direct the user to the fingerprint settings (requires API level 30 or higher)
                Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BiometricManager.Authenticators.BIOMETRIC_STRONG);
                try {
                    startActivity(enrollIntent);
                } catch (Exception e) {
                    Toast.makeText(this,
                            "No se puede abrir la configuración de huellas: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
                break;

            default:
                Toast.makeText(this,
                        "Estado de autenticación biométrica desconocido",
                        Toast.LENGTH_LONG).show();
                break;
        }

        return false;
    }

    /**
     * Sets up the biometric authentication components including the executor
     * and the BiometricPrompt with its callbacks.
     */
    private void setupBiometricAuth() {
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        // Use LoginActivity's navigation logic
                        startActivity(new Intent(BiometricAuthActivity.this, LoginActivity.class));
                        finish();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // If the error is not user cancellation, show the error
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                                errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            Toast.makeText(BiometricAuthActivity.this,
                                    "Error de autenticación: " + errString,
                                    Toast.LENGTH_SHORT).show();
                        }
                        // On authentication error, log out the user and go to login
                        sessionManager.clearSession();
                        startActivity(new Intent(BiometricAuthActivity.this, LoginActivity.class));
                        finish();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(BiometricAuthActivity.this,
                                "Autenticación fallida",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Configure the prompt dialog
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación Biométrica")
                .setSubtitle("Usa tu huella digital para acceder a la aplicación")
                .setNegativeButtonText("Cancelar")
                .build();
    }

    /**
     * Shows the biometric authentication prompt to the user.
     */
    private void showBiometricPrompt() {
        biometricPrompt.authenticate(promptInfo);
    }
}
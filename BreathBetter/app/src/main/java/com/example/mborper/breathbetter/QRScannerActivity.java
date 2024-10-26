package com.example.mborper.breathbetter;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;

/**
 * QRScannerActivity is an Android activity that allows scanning QR codes using the CodeScanner library.
 * It provides an interface to initiate scanning and handle the results obtained.
 *
 * @author Juan Diaz
 */
public class QRScannerActivity extends AppCompatActivity {

    private CodeScanner codeScanner; // Instance of the QR code scanner
    public static final String QR_RESULT = "qr_result"; // Key for the scan result

    /**
     * Called when the activity is created. Initializes the necessary components for QR code scanning.
     *
     * @param savedInstanceState The saved instance state of the activity, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Call the superclass method
        setContentView(R.layout.activity_qr_scanner); // Set the layout for the activity

        // Initialize the QR code scanner view
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        codeScanner = new CodeScanner(this, scannerView); // Create an instance of the scanner

        // Configure the scanner: define the callback to handle the scan result
        codeScanner.setDecodeCallback(result -> {
            runOnUiThread(() -> {
                // Return the result to the activity that called the scanner
                Intent resultIntent = new Intent();
                resultIntent.putExtra(QR_RESULT, result.getText()); // Add the scanned text to the intent
                setResult(Activity.RESULT_OK, resultIntent); // Set the activity result
                finish(); // Finish the activity
            });
        });

        // Handle errors during scanning
        codeScanner.setErrorCallback(error -> runOnUiThread(() -> {
            // Show a Toast message with the error
            Toast.makeText(this, "Error scanning: " + error.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }));
    }

    /**
     * Called when the activity is resumed. Starts the preview of the QR code scanner.
     */
    @Override
    protected void onResume() {
        super.onResume(); // Call the superclass method
        codeScanner.startPreview(); // Start the scanner preview
    }

    /**
     * Called when the activity is paused. Releases the scanner resources.
     */
    @Override
    protected void onPause() {
        codeScanner.releaseResources(); // Release scanner resources
        super.onPause(); // Call the superclass method
    }
}
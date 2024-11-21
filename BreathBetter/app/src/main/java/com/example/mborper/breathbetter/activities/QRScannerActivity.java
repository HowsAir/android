package com.example.mborper.breathbetter.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.example.mborper.breathbetter.R;

/**
 * QRScannerActivity is an Android activity that allows scanning QR codes using the CodeScanner library.
 * It provides an interface to initiate scanning and handle the results obtained.
 *
 * @since 2024-10-26
 * @author Juan Diaz
 * last updated: 2024-11-21
 */
public class QRScannerActivity extends AppCompatActivity {

    private CodeScanner codeScanner; // Instance of the QR code scanner
    public static final String QR_RESULT = "qr_result";

    /**
     * Called when the activity is created. Initializes the necessary components for QR code scanning.
     *
     * @param savedInstanceState The saved instance state of the activity, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        TextView tvError = findViewById(R.id.tvError);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        CodeScannerView scannerView = findViewById(R.id.scanner_view);
        codeScanner = new CodeScanner(this, scannerView);
        // Configure the scanner: define the callback to handle the scan result
        codeScanner.setDecodeCallback(result -> {
            runOnUiThread(() -> {
                // Return the result to the activity that called the scanner
                Intent resultIntent = new Intent();
                resultIntent.putExtra(QR_RESULT, result.getText()); // Add the scanned text to the intent
                setResult(Activity.RESULT_OK, resultIntent); // Set the activity result
                finish();
            });
        });

        ImageButton btnBack = findViewById(R.id.btnBack3);
        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        codeScanner.setErrorCallback(error -> runOnUiThread(() -> {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("Error escaneando");
        }));
    }

    /**
     * Called when the activity is resumed. Starts the preview of the QR code scanner.
     */
    @Override
    protected void onResume() {
        super.onResume();
        codeScanner.startPreview();
    }

    /**
     * Called when the activity is paused. Releases the scanner resources.
     */
    @Override
    protected void onPause() {
        codeScanner.releaseResources();
        super.onPause();
    }
}
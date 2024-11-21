package com.example.mborper.breathbetter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mborper.breathbetter.R;

/**
 * Activity that allows users to manually input a node ID as an alternative to QR scanning.
 * <p>
 * This activity provides a UI for entering a node ID manually, with input filters to ensure
 * only numeric characters are allowed and the length is limited to 5 digits.
 * Once validated, the node ID is returned as the result of the activity.
 *
 * @author Alejandro Rosado
 * @since 2024-11-01
 * last updated: 2024-11-21
 */
public class ManualInputActivity extends AppCompatActivity {

    private EditText nodeIdInput;
    public static final String MANUAL_RESULT = "manual_result";

    /**
     * Initializes the activity and sets up the input field and button.
     * <p>
     * Sets input filters on the node ID field to restrict the length and enforce numeric input,
     * and assigns a click listener to the confirm button to handle validation and submission.
     *
     * @param savedInstanceState the saved state of the activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_input);

        nodeIdInput = findViewById(R.id.nodeIdInput);
        Button confirmButton = findViewById(R.id.confirmButton);

        ImageButton btnBack = findViewById(R.id.btnBack4);
        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        // Set up input filters
        InputFilter[] filters = new InputFilter[]{
                // Limit length to 5 characters
                new InputFilter.LengthFilter(5),
                // Only allow numbers
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        for (int i = start; i < end; i++) {
                            if (!Character.isDigit(source.charAt(i))) {
                                return ""; // Block non-numeric characters
                            }
                        }
                        return null; // Allow numeric characters
                    }
                }
        };
        nodeIdInput.setFilters(filters);

        confirmButton.setOnClickListener(v -> validateAndSubmit());
    }

    /**
     * Validates the user input and submits the node ID.
     * <p>
     * This method performs the following steps:
     *      1. Retrieves and trims the text input from the node ID field.
     *      2. Checks if the input is empty. If it is, displays an error message.
     *      3. If valid, creates an intent with the node ID as an extra.
     *      4. Sets the result for the activity and finishes it.
     */
    private void validateAndSubmit() {
        String nodeId = nodeIdInput.getText().toString().trim();

        if (nodeId.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa un número", Toast.LENGTH_SHORT).show();
            return;
        }

        // Return the result similar to QRScannerActivity
        Intent resultIntent = new Intent();
        resultIntent.putExtra(MANUAL_RESULT, nodeId);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}

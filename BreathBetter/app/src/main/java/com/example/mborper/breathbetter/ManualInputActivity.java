package com.example.mborper.breathbetter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity that allows users to manually input a node ID as an alternative to QR scanning.
 *
 * @author [Your Name]
 */
public class ManualInputActivity extends AppCompatActivity {

    private EditText nodeIdInput;
    public static final String MANUAL_RESULT = "manual_result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_input);

        nodeIdInput = findViewById(R.id.nodeIdInput);
        Button confirmButton = findViewById(R.id.confirmButton);

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
                                return "";
                            }
                        }
                        return null;
                    }
                }
        };
        nodeIdInput.setFilters(filters);

        confirmButton.setOnClickListener(v -> validateAndSubmit());
    }

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
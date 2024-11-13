package com.example.mborper.breathbetter.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mborper.breathbetter.R;

/**
 * Handles displaying the Terms of Service and Privacy Policy for the HowsAir app.
 * This activity is used to show the full content of the terms and privacy policies to the user.
 * It retrieves the content from the app's string resources and displays it in a scrollable TextView.
 *
 * @author Alejandro Rosado
 * @since 2024-11-13
 */
public class TermsAndPrivacyActivity extends AppCompatActivity {

    /**
     * Initializes the activity. Sets up the UI components, loads the terms and privacy text,
     * and displays it in a scrollable TextView.
     *
     * @param savedInstanceState If the activity is being reinitialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_and_privacy);

        // Reference to the TextView where the Terms and Privacy Policy will be displayed
        TextView textView = findViewById(R.id.textViewTermsPrivacy);

        // Sets the text with the terms and privacy policy
        String termsAndPrivacyText = getString(R.string.terms_and_privacy_text);
        textView.setText(termsAndPrivacyText);
    }
}
package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.mborper.breathbetter.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Activity responsible for displaying the goal screen and handling bottom navigation interactions.
 * This activity provides navigation to different parts of the app using a BottomNavigationView.
 *
 * @author Alejandro Rosado
 * @since  2024-11-19
 */
public class GoalActivity extends AppCompatActivity {

    /**
     * Called when the activity is created.
     * <p>
     * Initializes the activity by setting the layout and setting up the bottom navigation menu.
     * @param savedInstanceState The saved state of the activity, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_goal);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.target);

        // Bottom navigation
        setupBottomNavigation();
    }

    /**
     * Configures the bottom navigation menu and defines the behavior when each item is selected.
     * <p>
     * Each navigation item (home, map, target, profile) will start a new activity or perform a transition
     * when selected. If the current item is the same as the one already selected, no action is performed.
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (item.getItemId() == R.id.map) {
                startActivity(new Intent(this, MapsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.target) {
                overridePendingTransition(0, 0);
                return true;
            } else if (item.getItemId() == R.id.profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

}

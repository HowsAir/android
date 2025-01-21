package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mborper.breathbetter.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {
    protected void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView == null) {
            Log.e("BaseActivity", "BottomNavigationView not found");
            return;
        }

        // Set the correct selected item based on the current activity
        bottomNavigationView.setSelectedItemId(getSelectedItemId());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (this.getClass() == getActivityClassForItemId(item.getItemId())) {
                return true;
            }

            Intent intent = new Intent(this, getActivityClassForItemId(item.getItemId()));
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            return true;
        });
    }

    protected int getSelectedItemId() {
        String currentClassName = this.getClass().getSimpleName();

        if (currentClassName.equals("MainActivity")) return R.id.home;
        if (currentClassName.equals("MapsActivity")) return R.id.map;
        if (currentClassName.equals("GoalActivity")) return R.id.target;
        if (currentClassName.equals("ProfileActivity")) return R.id.profile;

        return R.id.home;
    }

    private Class<? extends AppCompatActivity> getActivityClassForItemId(int itemId) {
        if (itemId == R.id.home) return MainActivity.class;
        if (itemId == R.id.map) return MapsActivity.class;
        if (itemId == R.id.target) return GoalActivity.class;
        if (itemId == R.id.profile) return ProfileActivity.class;
        return MainActivity.class;
    }

    private static String currentScreen = "HOME"; // Default value

    public static void setCurrentScreen(String screen) {
        currentScreen = screen;
    }

    public static String getCurrentScreen() {
        return currentScreen;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupBottomNavigation();
    }
}
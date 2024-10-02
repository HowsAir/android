package com.example.mborper.breathbetter;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.util.Log;
import android.view.View;


public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "DEVELOPMENT_LOG";

    private Intent serviceIntent = null;

    public void onStartServiceButtonClicked(View v) {
        Log.d(LOG_TAG, "Start service button clicked");

        if (this.serviceIntent != null) {
            // The service is already running
            return;
        }

        Log.d(LOG_TAG, "MainActivity: Starting the service");

        this.serviceIntent = new Intent(this, BeaconListeningService.class);

        this.serviceIntent.putExtra("waitTime", 5000L);

        // Use startForegroundService() instead of startService() for Android 8.0+
        startForegroundService(this.serviceIntent);

    } // onStartServiceButtonClicked

    public void onStopServiceButtonClicked(View v) {

        if (this.serviceIntent == null) {
            // The service is not running
            return;
        }

        stopService(this.serviceIntent);

        this.serviceIntent = null;

        Log.d(LOG_TAG, "Stop service button clicked");

    } // onStopServiceButtonClicked


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, "MainActivity: onCreate started");
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            Log.d(LOG_TAG, "MainActivity: onCreate finished");
            return insets;
        });
    }
}
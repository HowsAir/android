package com.example.mborper.breathbetter;


import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.example.mborper.breathbetter.bluetooth.BeaconListeningService;
import android.Manifest;

import android.content.pm.PackageManager;

import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.api.Measurement;
import com.example.mborper.breathbetter.bluetooth.IBeaconFrame;
import com.example.mborper.breathbetter.bluetooth.Utilities;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "DEVELOPMENT_LOG";
    private static final int PERMISSION_REQUEST_CODE = 8888;
    private Intent serviceIntent = null;
    private ApiService apiService;
    private BeaconListeningService beaconListeningService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        requestPermissions();
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void onStartServiceButtonClicked(View v) {
        Log.d(LOG_TAG, "Start service button clicked");

        if (this.serviceIntent != null) {
            return;
        }

        Log.d(LOG_TAG, "MainActivity: Starting the service");

        this.serviceIntent = new Intent(this, BeaconListeningService.class);
        this.serviceIntent.putExtra("targetDevice", "ManusBeacon"); // Replace with your beacon's name

        startForegroundService(this.serviceIntent);
        beaconListeningService = new BeaconListeningService();
    }

    public void onStopServiceButtonClicked(View v) {
        if (this.serviceIntent == null) {
            return;
        }

        stopService(this.serviceIntent);
        this.serviceIntent = null;
        beaconListeningService = null;
        Log.d(LOG_TAG, "Stop service button clicked");
    }

    public void onMakeRequestButtonClicked(View v) {
        if (beaconListeningService != null && beaconListeningService.getActualMeasurement() != null) {
            sendMeasurementToApi(beaconListeningService.getActualMeasurement());
        } else {
            Log.e(LOG_TAG, "BeaconListeningService or actualMeasurement is null");
        }
    }


    private void sendMeasurementToApi(Measurement measurement) {
        Call<Measurement> postCall = apiService.sendMeasurement(measurement);
        postCall.enqueue(new Callback<Measurement>() {
            @Override
            public void onResponse(Call<Measurement> call, Response<Measurement> response) {
                if (response.isSuccessful()) {
                    Log.d(LOG_TAG, "Measurement sent successfully");
                }
            }

            @Override
            public void onFailure(Call<Measurement> call, Throwable t) {
                Log.e(LOG_TAG, "Error sending measurement: " + t.getMessage());
            }
        });
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "onRequestPermissionResult(): permissions granted!");
            } else {
                Log.d(LOG_TAG, "onRequestPermissionResult(): Help: permissions NOT granted!");
            }
        }
    }
}
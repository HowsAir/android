package com.example.mborper.breathbetter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import android.Manifest;
import android.widget.Toast;

import com.example.mborper.breathbetter.bluetooth.BeaconListeningService;

import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.api.Measurement;
import com.example.mborper.breathbetter.bluetooth.BluetoothPermissionHandler;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "DEVELOPMENT_LOG";
    private static final int REQUEST_ENABLE_BT = 1;
    private Intent serviceIntent = null;
    private ApiService apiService;
    private static final String TARGET_UUID = "99AECF02-7F55-4405-9E8D-AFD81DC9407E";
    private BeaconListeningServiceConnection serviceConnection;
    private boolean isBound = false;

    private MutableLiveData<Measurement> lastMeasurementLiveData = new MutableLiveData<>();

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

        apiService = ApiClient.getClient().create(ApiService.class);
        serviceConnection = new BeaconListeningServiceConnection();

        // Observe the LiveData
        lastMeasurementLiveData.observe(this, this::updateUI);

        //checkBluetoothPermissions();
    }

    private void checkBluetoothPermissions() {
        if (BluetoothPermissionHandler.checkAndRequestBluetoothPermissions(this)) {
            // Permissions are granted, proceed with Bluetooth operations
            initializeBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (BluetoothPermissionHandler.handlePermissionResult(requestCode, permissions, grantResults)) {
            // All required permissions are granted
            initializeBluetooth();
        } else {
            showToast("Bluetooth and Location permissions are required for this app to function properly");
        }
    }

    private void initializeBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("This device doesn't support Bluetooth");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                // If we don't have BLUETOOTH_CONNECT permission, we should have already requested it in checkBluetoothPermissions()
                // So this else block should not be reached under normal circumstances
                showToast("Bluetooth permission is required");
            }
        } else {
            startAndBindService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startAndBindService();
            } else {
                showToast("Bluetooth is required for this app");
            }
        }
    }

    private void startAndBindService() {
        Log.d("adapter", "adapterenabled");
        if (serviceIntent == null) {
            serviceIntent = new Intent(this, BeaconListeningService.class);
            serviceIntent.putExtra("targetDeviceUUID", TARGET_UUID);
            ContextCompat.startForegroundService(this, serviceIntent);
        }
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    public void onStartServiceButtonClicked(View v) {
        checkBluetoothPermissions();
    }

    public void onStopServiceButtonClicked(View v) {
        if (serviceIntent != null) {
            if (isBound) {
                unbindService(serviceConnection);
                isBound = false;
            }
            stopService(serviceIntent);
            serviceIntent = null;
            showToast("Service stopped");
        }
    }

    public void onMakeRequestButtonClicked(View v) {
        Measurement lastMeasurement = lastMeasurementLiveData.getValue();
        if (lastMeasurement != null) {
            sendMeasurementToApi(lastMeasurement);
        } else {
            showToast("No measurement available to send");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private class BeaconListeningServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BeaconListeningService.LocalBinder binder = (BeaconListeningService.LocalBinder) service;
            BeaconListeningService beaconService = binder.getService();
            beaconService.setMeasurementCallback(measurement -> {
                lastMeasurementLiveData.postValue(measurement);
            });
            isBound = true;
            showToast("Service bound successfully");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            showToast("Service unbound");
        }
    }

    private void updateUI(Measurement measurement) {
        // Update UI elements with the new measurement data
        // For example:
        // TextView ppmTextView = findViewById(R.id.ppmTextView);
        // ppmTextView.setText("PPM: " + measurement.getPpm());
    }

    private void sendMeasurementToApi(Measurement measurement) {
        Call<Measurement> postCall = apiService.sendMeasurement(measurement);
        postCall.enqueue(new Callback<Measurement>() {
            @Override
            public void onResponse(Call<Measurement> call, Response<Measurement> response) {
                if (response.isSuccessful()) {
                    Log.d(LOG_TAG, "Measurement sent successfully");
                } else {
                    Log.e(LOG_TAG, "Error sending measurement to the API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Measurement> call, Throwable t) {
                Log.e(LOG_TAG, "Error sending measurement: " + t.getMessage());
            }
        });
    }
}
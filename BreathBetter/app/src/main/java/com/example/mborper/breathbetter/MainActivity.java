package com.example.mborper.breathbetter;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.MutableLiveData;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import com.example.mborper.breathbetter.bluetooth.BeaconListeningService;

import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.measurements.Measurement;
import com.example.mborper.breathbetter.bluetooth.BluetoothPermissionHandler;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @file MainActivity.java
 * @author Manuel Borregales
 * @date 2024-10-07
 * @description Main activity that handles the interaction between the UI and the Bluetooth beacon listening service.
 * It initializes Bluetooth, binds to the service, listens for measurements, and allows sending data to the API.
 */

public class MainActivity extends AppCompatActivity {

    /** Log tag for debugging. */
    private static final String LOG_TAG = "DEVELOPMENT_LOG";

    /** UUID for the target Bluetooth device. */
    private static final String TARGET_UUID = "MANU-EPSG-GTI-3A";

    private ApiService apiService;
    private static final int REQUEST_ENABLE_BT = 1;
    private BeaconListeningServiceConnection serviceConnection;
    private BeaconListeningService beaconService;
    private Intent serviceIntent = null;
    /** Tracks whether the service is bound to the activity. */
    private boolean isBound = false;
    /** Handles the mutable variable */
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    /** LiveData that holds the last received measurement and updates the UI accordingly. */
    private MutableLiveData<Measurement> lastMeasurementLiveData = new MutableLiveData<>();

    /**
     * Called when the activity is created. Initializes the UI and sets up the service connection and observer.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Enables edge-to-edge mode for the UI.
        setContentView(R.layout.activity_main);

        serviceConnection = new BeaconListeningServiceConnection();

        // Adjusts the padding based on system bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initializes the API client and service connection.
        apiService = ApiClient.getClient().create(ApiService.class);
        // Observes the LiveData for any updates to the last measurement.
        lastMeasurementLiveData.observe(this, this::updateUI);
    }

    /**
     * Class to handle the connection to the beacon service and retrieving of live changing data.
     */
    private class BeaconListeningServiceConnection implements ServiceConnection {
        /**
         * Called when the service is connected.
         *
         * @param name The name of the connected component.
         * @param service The IBinder returned by the bound service.
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BeaconListeningService.LocalBinder binder = (BeaconListeningService.LocalBinder) service;
            beaconService = binder.getService();
            beaconService.setMeasurementCallback(measurement -> {
                mainHandler.post(() -> {
                    lastMeasurementLiveData.setValue(measurement); // Updates LiveData with new measurement.
                });
            });
            isBound = true;
            showToast("Service bound successfully");

            // Retrieves the last known measurement from the service and updates the UI if available.
            Measurement lastMeasurement = beaconService.getLastMeasurement();
            if (lastMeasurement != null) {
                lastMeasurementLiveData.setValue(lastMeasurement);
                receiveAndSendMeasurement();
            }
        }

        /**
         * Called when the service is disconnected.
         *
         * @param name The name of the disconnected component.
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            showToast("Service unbound");
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            receiveAndSendMeasurement();
            mainHandler.postDelayed(this, 10000);
        }
    };

    /**
     * Starts the Bluetooth initialization process when the start service button is clicked.
     *
     * @param v The button view that was clicked.
     */
    public void onStartServiceButtonClicked(View v) {
        tryBluetoothInit(); // Attempts to initialize Bluetooth.
        mainHandler.post(runnable);
    }

    /**
     * Stops the service and unbinds it when the stop service button is clicked.
     *
     * @param v The button view that was clicked.
     */
    public void onStopServiceButtonClicked(View v) {
        if (serviceIntent != null) {
            try {
                if (isBound && beaconService != null) {
                    beaconService.stopService(); // This will trigger the complete cleanup
                    unbindService(serviceConnection);
                    isBound = false;
                }
                stopService(serviceIntent);
                serviceIntent = null;
                mainHandler.removeCallbacks(runnable);
                showToast("Service stopped");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error stopping service: " + e.getMessage());
                showToast("Error stopping service");
            }
        }
    }

    /**
     * Makes a request to the API with the last available measurement everytime a new measurement is received
     */
    public void receiveAndSendMeasurement() {
        Measurement lastMeasurement = lastMeasurementLiveData.getValue();
        if (lastMeasurement != null) {
            sendMeasurementToApi(lastMeasurement); // Sends the current measurement to the API.
        } else {
            showToast("No measurement available to send");
            if (beaconService != null) {
                lastMeasurement = beaconService.getLastMeasurement();
                if (lastMeasurement != null) {
                    sendMeasurementToApi(lastMeasurement);
                } else {
                    showToast("No measurement available from service");
                }
            } else {
                showToast("Service not bound");
            }
        }
    }

    /**
     * Attempts to initialize Bluetooth, checking and requesting necessary permissions.
     */
    private void tryBluetoothInit() {
        if (BluetoothPermissionHandler.checkAndRequestBluetoothPermissions(this)) {
            initializeBluetooth();
        }
    }

    /**
     * Handles the result of the permission request, it's called after accepting or rejecting a permission request.
     *
     *      Natural: requestCode
     *      [Texto]: Permissions    ---->   onRequestPermissionsResult()
     *      [Natural]: Granted permissions
     *
     * @param requestCode The request code passed in the request.
     * @param permissions The requested permissions.
     * @param grantResults The results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (BluetoothPermissionHandler.handlePermissionResult(requestCode, grantResults)) {
            initializeBluetooth();
        } else {
            showToast("Bluetooth and Location permissions are required for this app to function properly");
        }
    }

    /**
     * Initializes Bluetooth, enabling it if necessary and starting the service.
     */
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
                showToast("Bluetooth permission is required");
            }
        } else {
            startAndBindService();
        }
    }

    /**
     * Handles the result of the Bluetooth enable request.
     *
     *      Natural: requestCode
     *      Natural: resultCode    ---->   onActivityResult()
     *      Intent: data
     *
     * @param requestCode The request code.
     * @param resultCode The result code from the Bluetooth enable activity.
     * @param data Additional data, if any.
     */
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

    /**
     * Starts the beacon service and binds it to this activity.
     */
    private void startAndBindService() {
        if (serviceConnection == null) {
            serviceConnection = new BeaconListeningServiceConnection();
        }

        if (serviceIntent == null) {
            serviceIntent = new Intent(this, BeaconListeningService.class);
            serviceIntent.putExtra("targetDeviceUUID", TARGET_UUID);
            ContextCompat.startForegroundService(this, serviceIntent);
        }

        // Add null and binding checks
        if (serviceConnection != null && !isBound) {
            try {
                bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error binding service: " + e.getMessage());
                showToast("Error starting service: " + e.getMessage());
            }
        }
    }

    /**
     * Sends the provided measurement to the API via HTTP post request.
     *
     *      Measurement { ppm: Natural
     *                   temperature: Natural   ---> sendMeasurementToApi()
     *                   latitude: Real
     *                   longitude: Real }
     *
     * @param measurement The measurement to send.
     */
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


    /**
     * Called when the activity is destroyed. Unbinds the service if it is currently bound.
     */
    @Override
    protected void onDestroy() {
        try {
            if (isBound && serviceConnection != null) {
                unbindService(serviceConnection);
                isBound = false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in onDestroy: " + e.getMessage());
        }
        super.onDestroy();
        mainHandler.removeCallbacks(runnable);
    }

    /**
     * Updates the UI Textviews related to the last measurements taken .
     *
     * @param measurement The measurement to take data from.
     */
    private void updateUI(Measurement measurement) {
        TextView ppmTextView = findViewById(R.id.ppmTextView);
        TextView tempTextView = findViewById(R.id.tempTextView);
        TextView latLongTextView = findViewById(R.id.latLongTextView);

        if (measurement != null) {
            ppmTextView.setText("PPM: " + measurement.getPpm());
            tempTextView.setText("Temperature: " + measurement.getTemperature());
            latLongTextView.setText("Lat: " + measurement.getLatitude() + ", Long: " + measurement.getLongitude());
        } else {
            ppmTextView.setText("PPM: N/A");
            tempTextView.setText("Temperature: N/A");
            latLongTextView.setText("Lat: N/A, Long: N/A");
        }
    }

    /**
     * Shows a temporal message on the screen to let the user know something.
     *
     * @param message the message to show on the screen.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
package com.example.mborper.breathbetter.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.models.Node;
import com.example.mborper.breathbetter.bluetooth.BeaconListeningService;

import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.login.SessionManager;
import com.example.mborper.breathbetter.measurements.Measurement;
import com.example.mborper.breathbetter.bluetooth.BluetoothPermissionHandler;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.example.mborper.breathbetter.bluetooth.BuzzerControl;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Date;

/**
 * Main activity that handles the interaction between the UI and the Bluetooth beacon listening service.
 * It initializes Bluetooth, binds to the service, listens for measurements, and allows sending data to the API.
 *
 * @author Manuel Borregales
 * @author Alejandro Rosado
 * @since  2024-10-07
 * last edited: 2024-11-10
 */
public class MainActivity extends AppCompatActivity {

    /** Log tag for debugging. */
    private static final String LOG_TAG = "DEVELOPMENT_LOG";

    /** UUID for the target Bluetooth device. */
    private static final String TARGET_UUID = "MANU-EPSG-GTI-3A";
    //private static final String TARGET_UUID = "4d414e55-2d45-5053-472d-4754492d3341";


    private ApiService apiService;

    /** Handles the mutable variable */
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    /** LiveData that holds the last received measurement and updates the UI accordingly. */
    private MutableLiveData<Measurement> lastMeasurementLiveData = new MutableLiveData<>();

    private static final int REQUEST_ENABLE_BT = 1;
    private BeaconListeningServiceConnection serviceConnection;
    private BeaconListeningService beaconService;
    private Intent serviceIntent = null;
    /** Tracks whether the service is bound to the activity. */
    private boolean isBound = false;

    private SessionManager sessionManager;

    private BuzzerControl buzzerControl;

    private boolean isBuzzerOn = false;

    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 2; // Different from REQUEST_ENABLE_BT

    private static final int REQUEST_CODE_BIOMETRIC_AUTH = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(this).create(ApiService.class);

        if(!sessionManager.isLoggedIn()){
            Log.d("DEBUG","Usuario no loggeado");
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            return;
        } else if(sessionManager.getNodeId() == null){
            Log.d("DEBUG","Obteniendo nodo");
            //Obtain userNode from API in order to check if user has got a node later as well
            getUserNode();
        } else{
            startBiometricAuthIfAvailable();
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        AdjustPadding();

        serviceConnection = new BeaconListeningServiceConnection();
        lastMeasurementLiveData.observe(this, this::updateUI);

        // Initialize buzzer control directly
        buzzerControl = new BuzzerControl(this, new BuzzerControl.Callback() {
            @Override
            public void onPermissionDenied() {
                updateBuzzerButtonState(false);
            }

            @Override
            public void onBluetoothNotSupported() {
                updateBuzzerButtonState(false);
            }

            @Override
            public void onSuccess() {}

            @Override
            public void onFailure(String error) {
                updateBuzzerButtonState(false);
            }
        });
        setupBuzzerButton();
    }

    /**
     * Method to start biometric authentication only if it is available.
     */
    private void startBiometricAuthIfAvailable() {
        BiometricAuthActivity biometricAuthActivity = new BiometricAuthActivity();

        if (!biometricAuthActivity.isBiometricAvailable(this)) {
            showToast("Biometric auth is not available");
        } else {
            Log.d("DEBUG", "Starting BiometricAuthActivity");
            Intent intent = new Intent(this, BiometricAuthActivity.class);
            startActivityForResult(intent, REQUEST_CODE_BIOMETRIC_AUTH);
        }
    }

    /**
     * Handles the process of retrieving the node associated with the authenticated user.
     * Sends a request to the backend to fetch the node details and manages the response
     * or any network errors that may occur during the call.
     */
    public void getUserNode() {
        apiService.getUserNode().enqueue(new Callback<JsonObject>() {

            /**
             * Handles the response from the getUserNode request. If successful, it processes
             * the node data. If unsuccessful, it notifies the user about the failure.
             *
             * @param call The call to the API that was executed.
             * @param response The response returned by the API call.
             */
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null && response.body().has("node")) {
                    JsonObject jsonResponse = response.body();

                    JsonObject nodeJson = jsonResponse.getAsJsonObject("node");
                    Node userNode = new Gson().fromJson(nodeJson, Node.class);

                    sessionManager.saveNodeId(Integer.toString(userNode.getId()));
                    showToast("Node retrieved successfully: " + userNode.getId());

                    startBiometricAuthIfAvailable();
                } else if (response.code() == 404) {
                    sessionManager.clearNodeId();
                    showToast("Ahora vincula tu nuevo nodo");

                    startActivity(new Intent(MainActivity.this, QRExplanationActivity.class));
                } else {
                    showToast("Error desconocido, inténtalo de nuevo o contáctanos");
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            }

            /**
             * Handles network failures during the node retrieval process.
             * Displays an error message with details about the failure.
             *
             * @param call The call to the API that was attempted.
             * @param t The throwable representing the network error.
             */
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                showToast("Error de red, comprueba tu conexión e inténtalo otra vez");
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });
    }

    /**
     * Updates the button state for toggling the buzzer.
     * <p>
     * Changes the text on the button based on the buzzer's current state
     * and updates the internal `isBuzzerOn` flag to match.
     *
     * @param isOn true if the buzzer is currently on, false if it is off
     */
    private void updateBuzzerButtonState(boolean isOn) {
        Button toggleButton = findViewById(R.id.toggleBuzzer);
        isBuzzerOn = isOn;
        toggleButton.setText(isOn ? R.string.Stop_Find_Node : R.string.Find_Node);
    }

    /**
     * Sets up the UI button for controlling the buzzer, verifying permissions before
     * attempting any Bluetooth operations.
     */
    private void setupBuzzerButton() {
        Button toggleButton = findViewById(R.id.toggleBuzzer);
        toggleButton.setOnClickListener(v -> {
            // Check all permissions including BLUETOOTH_ADVERTISE
            if (!BluetoothPermissionHandler.checkAndRequestBluetoothPermissions(this)) {
                showToast("Waiting for all Bluetooth permissions...");
                updateBuzzerButtonState(false);
                return;
            }

            if (!isBuzzerOn) {
                buzzerControl.turnOnBuzzer();
                toggleButton.setText(R.string.Stop_Find_Node);
            } else {
                buzzerControl.turnOffBuzzer();
                toggleButton.setText(R.string.Find_Node);
            }
            isBuzzerOn = !isBuzzerOn;
        });
    }

    /**
     * Class to handle the logout button and process its click.
     */
    public void onLogoutButtonClicked(View v) {
        // Stop service and clean up
        onStopServiceButtonClicked(null);

        // Clear session
        sessionManager.clearSession();

        // Redirect to login
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
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

    /**
     * Adjusts the padding of the main view based on system bar insets.
     * <p>
     * This method sets an `OnApplyWindowInsetsListener` to automatically apply
     * padding to account for the system bars (e.g., status and navigation bars),
     * ensuring the main view content does not overlap with these UI elements.
     */
    private void AdjustPadding() {
        // Adjusts the padding based on system bars.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Function to store and send automatically measurements to the API without a button every 10 seconds
     */
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            receiveAndSendMeasurement();
            mainHandler.postDelayed(this, 10000);
        }
    };

    /**
     * Makes a request to the API with the last available measurement everytime a new measurement is received
     */
    public void receiveAndSendMeasurement() {
        Measurement lastMeasurement = lastMeasurementLiveData.getValue();
        if (lastMeasurement != null) {
            sendMeasurementToApi(lastMeasurement); // Sends the current measurement to the API.
        } else {
            if (beaconService != null) {
                lastMeasurement = beaconService.getLastMeasurement();
                if (lastMeasurement != null) {
                    sendMeasurementToApi(lastMeasurement);
                } else {
                    //showToast("No measurement available from service");
                }
            }
        }
    }

    /**
     * Starts the Bluetooth initialization process when the start service button is clicked.
     * Checks for required permissions before starting the service,
     *
     * @param v The button view that was clicked.
     */
    public void onStartServiceButtonClicked(View v) {
        if (BluetoothPermissionHandler.checkAndRequestBluetoothPermissions(this)) {
            startAndBindService();
            mainHandler.post(runnable);
        } else {
            showToast("Waiting for permission approval...");
        }
    }

    /**
     * Handles the result of the permission request, it's called after accepting or rejecting a permission request.
     * <p>
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
            if (beaconService != null) {
                beaconService.initializeBluetooth();
            } else {
                // If permissions were granted from Start Searching button
                startAndBindService();
                mainHandler.post(runnable);
            }
        } else {
            showToast("Bluetooth and Location permissions are required for this app to function properly");
        }
    }

    /**
     * Handles the result of the Bluetooth enable request.
     * <p>
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
        else if(requestCode == REQUEST_CODE_BIOMETRIC_AUTH){
            if(resultCode == RESULT_OK){
                showToast("Biometric auth OK, access the app");
            } else{
                //Build again MainActivity
                recreate();
            }
        }
    }

    /**
     * Starts the beacon service and binds it to this activity, also sends the targetUUID.
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
     * <p>
     *      Measurement { o3Value: Natural
     *                                  ---> sendMeasurementToApi()
     *                   latitude: Real
     *                   longitude: Real }
     *
     * @param measurement The measurement to send.
     */
    private void sendMeasurementToApi(Measurement measurement) {
        Call<Measurement> postCall = apiService.sendMeasurement(measurement);
        postCall.enqueue(new Callback<Measurement>() {
            @Override
            public void onResponse(@NonNull Call<Measurement> call, @NonNull Response<Measurement> response) {
                if (response.isSuccessful()) {
                    Log.d(LOG_TAG, "Measurement sent successfully");
                } else {
                    Log.e(LOG_TAG, "Error sending measurement to the API: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Measurement> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "Error sending measurement: " + t.getMessage());
            }
        });
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
     * Updates the UI Textview related to the last measurements taken .
     *
     * @param measurement The measurement to take data from.
     */
    private void updateUI(Measurement measurement) {
        TextView ppmTextView = findViewById(R.id.ppmTextView);
        TextView latLongTextView = findViewById(R.id.latLongTextView);

        if (measurement != null) {
            ppmTextView.setText("O3 Value (PPM): " + measurement.getO3Value());
            latLongTextView.setText("Lat: " + measurement.getLatitude() + ", Long: " + measurement.getLongitude());
        } else {
            ppmTextView.setText("PPM: N/A");
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
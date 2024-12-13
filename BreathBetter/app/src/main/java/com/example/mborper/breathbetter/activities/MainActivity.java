package com.example.mborper.breathbetter.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
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
import android.widget.ImageView;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.models.Node;
import com.example.mborper.breathbetter.bluetooth.BeaconListeningService;

import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.graphs.ChartConfigHelper;
import com.example.mborper.breathbetter.login.SessionManager;
import com.example.mborper.breathbetter.measurements.GasAlertManager;
import com.example.mborper.breathbetter.measurements.Measurement;
import com.example.mborper.breathbetter.bluetooth.BluetoothPermissionHandler;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.mborper.breathbetter.bluetooth.BuzzerControl;
import com.example.mborper.breathbetter.measurements.NodeConnectionState;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


/**
 * Main activity that handles the interaction between the UI and the Bluetooth beacon listening service.
 * It initializes Bluetooth, binds to the service, listens for measurements, and allows sending data to the API.
 *
 * @author Manuel Borregales
 * @author Alejandro Rosado
 * @since  2024-10-07
 * last edited: 2024-12-12
 */
public class MainActivity extends BaseActivity
        implements NodeConnectionState.ConnectionStatusListener {

    private NotificationManager notificationManager;
    private static final String CONNECTION_CHANNEL_ID = "CONNECTION_CHANNEL";

    // New flag to track first release
    private static final String PREFS_NAME = "AppPreferences";
    private static final String FIRST_LAUNCH_KEY = "isFirstLaunch";

    /**
     * Log tag for debugging.
     */
    private static final String LOG_TAG = "DEVELOPMENT_LOG";

    /**
     * UUID for the target Bluetooth device.
     */
    private static final String TARGET_UUID = "MANU-EPSG-GTI-3A";
    //private static final String TARGET_UUID = "4d414e55-2d45-5053-472d-4754492d3341";

    /**
     * Handler for updating the distance traveled periodically
     */
    private final Handler distanceUpdateHandler = new Handler(Looper.getMainLooper());

    private ApiService apiService;

    private Handler dashboardUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable dashboardUpdateRunnable;

    /**
     * Handles the mutable variable
     */
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    /**
     * LiveData that holds the last received measurement and updates the UI accordingly.
     */
    private MutableLiveData<Measurement> lastMeasurementLiveData = new MutableLiveData<>();

    private static final int REQUEST_ENABLE_BT = 1;
    private BeaconListeningServiceConnection serviceConnection;
    private BeaconListeningService beaconService;
    private Intent serviceIntent = null;
    /**
     * Tracks whether the service is bound to the activity.
     */
    private boolean isBound = false;

    private SessionManager sessionManager;

    private BuzzerControl buzzerControl;

    private boolean isBuzzerOn = false;

    private static final int REQUEST_CODE_BIOMETRIC_AUTH = 3;
    private static final int REQUEST_CODE_BLUETOOTH_PERMISSIONS = 2;

    private BarChart barChart;
    private int currentHour = 0; // Comienza a la medianoche (00:00)
    private ArrayList<BarEntry> entries;
    private ArrayList<Integer> colorList = new ArrayList<>();

    private ArrayList<String> hours;

    private ImageView cautionAirIcon;
    private TextView textCaution;
    private TextView ppmTextView;

    // Define a constant for the dangerous PPM threshold
    private static final float DANGEROUS_PPM_THRESHOLD = 100f;

    private static boolean hasBiometricAuthenticationBeenPerformed = false;

    private GasAlertManager gasAlertManager;

    private NodeConnectionState connectionState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient(this).create(ApiService.class);
        gasAlertManager = new GasAlertManager(this);
        this.connectionState = NodeConnectionState.getInstance();

        NodeConnectionState connectionState = NodeConnectionState.getInstance();
        connectionState.setConnectionStatusListener(this);

        // Crear canal de notificaciones para conexión
        createConnectionNotificationChannel();

        overridePendingTransition(0, 0);

        // If not logged in, redirect to login
        if (!sessionManager.isLoggedIn()) {
            Log.d("DEBUG", "Usuario no loggeado");
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Check if it is the first start
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(FIRST_LAUNCH_KEY, true);

        // Always try to get the node if not already retrieved
        if (sessionManager.getNodeId() == null) {
            Log.d("DEBUG", "Obteniendo nodo");
            getUserNode();
            return;
        }

        // Check and request Bluetooth permissions first
        if (!BluetoothPermissionHandler.checkAndRequestBluetoothPermissions(this)) {
            // Permissions will be requested, and onRequestPermissionsResult will be called
            return;
        }

        // If permissions are granted, proceed with setup
        setupMainActivity(isFirstLaunch);

        setCurrentScreen("HOME");

        // Bottom navigation
        setupBottomNavigation();



        cautionAirIcon = findViewById(R.id.caution_air_icon);
        textCaution = findViewById(R.id.text_caution);
        ppmTextView = findViewById(R.id.ppmTextView);

        // Initially hide caution views
        cautionAirIcon.setVisibility(View.GONE);
        textCaution.setVisibility(View.GONE);

        View airQualityGradientSlider = findViewById(R.id.air_quality_gradient_slider);
        View sliderIndicator = findViewById(R.id.slider_indicator);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupBottomNavigation();
    }

    /**
     * Creates a connection notification channel for the node.
     * <p>
     * Int -> createConnectionNotificationChannel() -> void
     */
    private void createConnectionNotificationChannel() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(
                CONNECTION_CHANNEL_ID,
                "Conexión del Nodo",
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager.createNotificationChannel(channel);
    }

    /**
     * Handles the event when the connection to the node is lost.
     * <p>
     * void -> onConnectionLost() -> void
     */
    @Override
    public void onConnectionLost() {
        // Mostrar notificación de pérdida de conexión
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CONNECTION_CHANNEL_ID)
                .setContentTitle("Conexión del Nodo Perdida")
                .setContentText("No se están recibiendo mediciones del nodo")
                .setSmallIcon(R.drawable.howsair_logo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1, builder.build());
    }

    /**
     * Handles the event when the connection to the node is restored.
     * <p>
     * void -> onConnectionRestored() -> void
     */
    @Override
    public void onConnectionRestored() {
        // Mostrar notificación de reconexión
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CONNECTION_CHANNEL_ID)
                .setContentTitle("Conexión del Nodo Restaurada")
                .setContentText("Se han reanudado las mediciones del nodo")
                .setSmallIcon(R.drawable.howsair_logo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(2, builder.build());
    }

    /**
     * Fully initializes the MainActivity by setting up all UI components,
     * service connections, and event listeners.
     * <p>
     * This method handles:
     * - Enabling edge-to-edge layout
     * - Setting the content view
     * - Configuring bottom navigation
     * - Setting up service connections for beacon listening
     * - Initializing buzzer control
     * - Setting up biometric authentication
     * <p>
     * Called after successful login and node linking to ensure all
     * necessary components are properly configured before displaying
     * the main application interface.
     */
    private void setupMainActivity(boolean shouldAuthenticate) {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Automatically start and bind the Bluetooth service
        startAndBindServiceAutomatically();

        serviceConnection = new BeaconListeningServiceConnection();

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
            public void onSuccess() {
            }

            @Override
            public void onFailure(String error) {
                updateBuzzerButtonState(false);
            }
        });
        setupBuzzerButton();

        // Setup periodic dashboard data fetch
        dashboardUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                fetchDashboardData();
                dashboardUpdateHandler.postDelayed(this, 30000); // 30 seconds
            }
        };
        dashboardUpdateHandler.post(dashboardUpdateRunnable);

        // Initialize line chart
        barChart = findViewById(R.id.barChart);
        ChartConfigHelper.setupChartBasics(barChart);
        ChartConfigHelper.configureYAxis(barChart);

        // Only require biometric authentication on first launch
        // and if it hasn't been performed before in this session
        if (shouldAuthenticate && !hasBiometricAuthenticationBeenPerformed) {
            startBiometricAuthIfAvailable();
        }
    }

    /**
     * Fetches the dashboard data from the API.
     * <p>
     * void -> fetchDashboardData() -> void
     */
    private void fetchDashboardData() {
        apiService.getDashboardData().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseData = response.body().getAsJsonObject("data");

                    if (responseData != null) {
                        JsonObject lastAirQuality = responseData.getAsJsonObject("lastAirQualityReading");
                        JsonObject airQualityReadingsInfo = responseData.getAsJsonObject("airQualityReadingsInfo");

                        if (airQualityReadingsInfo != null && airQualityReadingsInfo.has("overallAirQuality")) {
                            String overallAirQuality = airQualityReadingsInfo.get("overallAirQuality").getAsString();

                            // Call the new method to update air quality display
                            updateAirQualityDisplay(overallAirQuality);
                        }

                        JsonArray airQualityReadings = airQualityReadingsInfo != null
                                ? airQualityReadingsInfo.getAsJsonArray("airQualityReadings")
                                : new JsonArray();

                        int todayDistance = responseData.has("todayDistance")
                                ? responseData.get("todayDistance").getAsInt()
                                : 0;

                        // Check for null or empty objects before accessing
                        if (lastAirQuality != null &&
                                lastAirQuality.has("timestamp") &&
                                lastAirQuality.has("ppmValue") &&
                                lastAirQuality.has("gas")) {

                            // Update the UI dashboard data
                            updateUIDashboardData(
                                    lastAirQuality.get("timestamp").getAsString(),
                                    lastAirQuality.get("ppmValue").getAsFloat(),
                                    lastAirQuality.get("gas").getAsString(),
                                    todayDistance
                            );

                            // Update chart only if there are readings
                            if (airQualityReadings.size() > 0) {
                                ChartConfigHelper.setupChartBasics(barChart);
                                ChartConfigHelper.configureYAxis(barChart);
                                ChartConfigHelper.updateBarChart(barChart, airQualityReadings, MainActivity.this);
                            } else {
                                Log.w(LOG_TAG, "No air quality readings available");
                            }
                        } else {
                            Log.e(LOG_TAG, "Incomplete last air quality reading data");
                        }
                    } else {
                        Log.e(LOG_TAG, "Data object is null in the API response");
                    }
                } else {
                    Log.e(LOG_TAG, "Error in API response: Code " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(LOG_TAG, "API call failed: " + t.getMessage());
            }
        });
    }

    /**
     * Updates the display of air quality based on the overall air quality status.
     * <p>
     * String -> updateAirQualityDisplay(String overallAirQuality) -> void
     * @param overallAirQuality The overall air quality status (Good, Regular, Bad)
     */
    private void updateAirQualityDisplay(String overallAirQuality) {
        ImageView airQualityIcon = findViewById(R.id.air_quality_icon);
        TextView airQualityText = findViewById(R.id.air_quality_text);
        TextView title_todays_air_quality = findViewById(R.id.title_todays_air_quality);

        airQualityText.setVisibility(View.VISIBLE);
        airQualityIcon.setVisibility(View.VISIBLE);
        title_todays_air_quality.setVisibility(View.VISIBLE);

        switch (overallAirQuality) {
            case "Good":
                airQualityIcon.setImageResource(R.drawable.good_quality);
                airQualityText.setText("Buena");
                airQualityText.setTextColor(Color.parseColor("#49B500"));
                break;
            case "Regular":
                airQualityIcon.setImageResource(R.drawable.regular_quality);
                airQualityText.setText("Regular");
                airQualityText.setTextColor(Color.parseColor("#ffdd00"));
                break;
            case "Bad":
                airQualityIcon.setImageResource(R.drawable.toxic_quality);
                airQualityText.setText("Mala");
                airQualityText.setTextColor(Color.parseColor("#DC2626"));
                break;
            default:
                // Optional: handle unknown air quality status
                airQualityIcon.setImageResource(R.drawable.good_quality);
                airQualityText.setText("N/A");
                airQualityText.setTextColor(Color.GRAY);
                break;
        }
    }

    /**
     * Converts a UTC timestamp to the local date and time format.
     * <p>
     * String -> convertToLocalDateTime(String utcTimestamp) -> String
     * @param utcTimestamp The UTC timestamp to convert
     * @return The formatted local date and time
     */
    private String convertToLocalDateTime(String utcTimestamp) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(utcTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            LocalDateTime localDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM");
            return localDateTime.format(formatter);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error converting timestamp: " + e.getMessage());
            return "N/A";
        }
    }

    /**
     * Updates the air quality warning based on the PPM value.
     * <p>
     * float -> updateAirQualityWarning(float ppmValue) -> void
     * @param ppmValue The PPM value to check against the threshold
     */
    private void updateAirQualityWarning(float ppmValue) {
        // Update PPM text view
        ppmTextView.setText(String.valueOf(ppmValue));

        // Check if the measurement is in the dangerous range
        if (ppmValue > DANGEROUS_PPM_THRESHOLD) {
            cautionAirIcon.setVisibility(View.VISIBLE);
            textCaution.setVisibility(View.VISIBLE);
        } else {
            cautionAirIcon.setVisibility(View.GONE);
            textCaution.setVisibility(View.GONE);
        }
    }

    /**
     * Automatically starts and binds the Bluetooth service without manual intervention.
     * Checks for necessary permissions before starting the service.
     */
    private void startAndBindServiceAutomatically() {
        if (BluetoothPermissionHandler.checkAndRequestBluetoothPermissions(this)) {
            startAndBindService();
            mainHandler.post(runnable);
        } else {
            showToast("Permisos necesarios...");
        }
    }

    /**
     * Method to start biometric authentication only if it is available.
     */
    private void startBiometricAuthIfAvailable() {
        BiometricAuthActivity biometricAuthActivity = new BiometricAuthActivity();

        if (!biometricAuthActivity.isBiometricAvailable(this)) {
            showToast("Biometric auth is not available");
            hasBiometricAuthenticationBeenPerformed = true;
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
                    //showToast("Node retrieved successfully: " + userNode.getId());

                    runOnUiThread(() -> {
                        // Get first start status
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        boolean isFirstLaunch = prefs.getBoolean(FIRST_LAUNCH_KEY, true);
                        setupMainActivity(isFirstLaunch);
                    });
                } else if (response.code() == 404) {
                    sessionManager.clearNodeId();
                    //showToast("Ahora vincula tu nuevo nodo");
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

        // Solo envía medidas si está conectado
        if (lastMeasurement != null && connectionState.isConnected()) {
            sendMeasurementToApi(lastMeasurement);
        } else {
            Log.d(LOG_TAG, "No measurement sent. Connection lost or no measurement available.");
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

        if (requestCode == REQUEST_CODE_BLUETOOTH_PERMISSIONS) {
            if (BluetoothPermissionHandler.handlePermissionResult(requestCode, grantResults)) {
                // Permissions granted, proceed with setup
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                boolean isFirstLaunch = prefs.getBoolean(FIRST_LAUNCH_KEY, true);
                setupMainActivity(isFirstLaunch);
            } else {
                // Permissions denied, show a message or handle accordingly
                Toast.makeText(this, "Bluetooth permissions are required", Toast.LENGTH_LONG).show();
            }
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

        if (requestCode == REQUEST_CODE_BIOMETRIC_AUTH) {
            if (resultCode == RESULT_OK) {
                // Successful authentication
                hasBiometricAuthenticationBeenPerformed = true;
            } else if (resultCode == RESULT_CANCELED) {
                // Authentication failed or cancelled
                recreate();
            }
        }
    }

    /**
     * Resets the biometric authentication flag.
     */
    public static void resetBiometricAuthenticationFlag() {
        hasBiometricAuthenticationBeenPerformed = false;
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
                    Log.d(LOG_TAG, "Measurement sent successfully.");
                } else {
                    Log.e(LOG_TAG, "API returned error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Measurement> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "Failed to send measurement: " + t.getMessage());
            }
        });
    }

    /**
     * Called when the activity is destroyed. Unbinds the service if it is currently bound.
     */
    @Override
    protected void onDestroy() {
        // Remove all callbacks to prevent memory leaks
        mainHandler.removeCallbacks(runnable);

        // Remove dashboard update callbacks to prevent memory leaks
        if (dashboardUpdateHandler != null && dashboardUpdateRunnable != null) {
            dashboardUpdateHandler.removeCallbacks(dashboardUpdateRunnable);
        }

        try {
            if (isBound && serviceConnection != null) {
                unbindService(serviceConnection);
                isBound = false;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in onDestroy: " + e.getMessage());
        }
        super.onDestroy();
    }

    /**
     * Updates the UI with the latest dashboard data including air quality, timestamp, and distance.
     * <p>
     * The method updates various UI elements such as air quality information, timestamp, and distance traveled.
     * @param timestamp The timestamp of the last reading.
     * @param proportionalValue The air quality proportional value (PPM).
     * @param gas The type of gas detected.
     * @param todayDistance The distance traveled today.
     */
    private void updateUIDashboardData(String timestamp, float proportionalValue, String gas, int todayDistance) {
        TextView ppmTextView = findViewById(R.id.ppmTextView);
        TextView textPpm = findViewById(R.id.text_ppm);
        TextView textLastDate = findViewById(R.id.text_last_date);
        //TextView textLastDistanceDate = findViewById(R.id.text_last_distance_date);
        TextView textDistanceTraveled = findViewById(R.id.textDistanceTraveled);
        TextView textMeters = findViewById(R.id.text_meters);

        // Actualiza los valores
        ppmTextView.setText(String.valueOf(proportionalValue));
        textPpm.setText(gas != null ? "ppm\n" + gas : "N/A");

        // Convierte la marca de tiempo a hora y fecha local
        String localDateTime = convertToLocalDateTime(timestamp);
        textLastDate.setText(localDateTime);
        //textLastDistanceDate.setText(localDateTime);
        updateAirQualityGradientSlider(proportionalValue);

        // Maneja la conversión de la distancia
        if (todayDistance > 999) {
            float distanceInKm = todayDistance / 1000.0f;
            textDistanceTraveled.setText(String.format("%.2f", distanceInKm));
            textMeters.setText("km");
        } else {
            textDistanceTraveled.setText(String.valueOf(todayDistance));
            textMeters.setText("m");
        }
    }

    /**
     * Updates the air quality gradient slider based on the given PPM value.
     * <p>
     * This method adjusts the position and color of the slider indicator based on the air quality (PPM value).
     * @param ppmValue The air quality proportional value (PPM).
     */
    private void updateAirQualityGradientSlider(float ppmValue) {
        View gradientSlider = findViewById(R.id.air_quality_gradient_slider);
        View sliderIndicator = findViewById(R.id.slider_indicator);

        // Asegurar que el valor esté entre 0 y 100
        ppmValue = Math.min(Math.max(ppmValue, 0), 100);

        // Obtener el ancho del slider
        int sliderWidth = gradientSlider.getWidth();

        // Calcular la posición del indicador
        int leftMargin = calculateIndicatorPosition(ppmValue, sliderWidth, sliderIndicator.getWidth());

        // Actualizar la posición del indicador
        sliderIndicator.setX(leftMargin);

        // Cambiar color dinámicamente
        int baseColor = interpolateColorBasedOnPPM(ppmValue); // Método para calcular el color
        updateSliderIndicatorDrawable(sliderIndicator, baseColor);
    }

    /**
     * Calculates the position of the indicator on the slider based on the PPM value.
     * @param ppmValue The air quality proportional value (PPM).
     * @param sliderWidth The width of the slider.
     * @param indicatorWidth The width of the indicator.
     * @return The position of the indicator in pixels.
     */
    private int calculateIndicatorPosition(float ppmValue, int sliderWidth, int indicatorWidth) {
        return (int) ((ppmValue / 100f) * sliderWidth) - (indicatorWidth / 2);
    }

    /**
     * Interpolates a color based on the PPM value.
     * <p>
     * Colors are interpolated between green (low PPM) and red (high PPM).
     * @param ppmValue The air quality proportional value (PPM).
     * @return The interpolated color.
     */
    private int interpolateColorBasedOnPPM(float ppmValue) {
        if (ppmValue <= 50) {
            float ratio = ppmValue / 50f;
            return interpolateColor(
                    Color.parseColor("#16A34A"), // Verde
                    Color.parseColor("#EAB308"), // Amarillo
                    ratio
            );
        } else {
            float ratio = (ppmValue - 50) / 50f;
            return interpolateColor(
                    Color.parseColor("#EAB308"), // Amarillo
                    Color.parseColor("#DC2626"), // Rojo
                    ratio
            );
        }
    }

    /**
     * Updates the drawable background of the slider indicator with a dynamic color.
     * @param sliderIndicator The slider indicator view.
     * @param baseColor The base color for the indicator.
     */
    private void updateSliderIndicatorDrawable(View sliderIndicator, int baseColor) {
        GradientDrawable drawable = (GradientDrawable) sliderIndicator.getBackground();

        // Establece el color del fondo (solid)
        drawable.setColor(baseColor);

        // Genera un tono más oscuro para el borde (stroke)
        int darkerColor = darkenColor(baseColor, 0.85f); // Oscurecer un 15%
        drawable.setStroke(8, darkerColor); // Actualizar el color del borde
    }

    /**
     * Darkens the given color by a specified factor.
     * @param color The base color.
     * @param factor The factor by which to darken the color.
     * @return The darkened color.
     */
    private int darkenColor(int color, float factor) {
        int r = Math.max((int) (Color.red(color) * factor), 0);
        int g = Math.max((int) (Color.green(color) * factor), 0);
        int b = Math.max((int) (Color.blue(color) * factor), 0);
        return Color.rgb(r, g, b);
    }

    /**
     * Interpolates between two colors based on the given ratio.
     * @param startColor The starting color.
     * @param endColor The ending color.
     * @param ratio The ratio for interpolation.
     * @return The resulting interpolated color.
     */
    private int interpolateColor(int startColor, int endColor, float ratio) {
        int r = interpolateColorComponent(Color.red(startColor), Color.red(endColor), ratio);
        int g = interpolateColorComponent(Color.green(startColor), Color.green(endColor), ratio);
        int b = interpolateColorComponent(Color.blue(startColor), Color.blue(endColor), ratio);
        return Color.rgb(r, g, b);
    }

    /**
     * Interpolates a single color component (Red, Green, or Blue) based on the ratio.
     * @param start The starting color component.
     * @param end The ending color component.
     * @param ratio The ratio for interpolation.
     * @return The interpolated color component.
     */
    private int interpolateColorComponent(int start, int end, float ratio) {
        return Math.round(start + ratio * (end - start));
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
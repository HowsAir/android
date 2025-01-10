package com.example.mborper.breathbetter.bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.Manifest;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.activities.MainActivity;
import com.example.mborper.breathbetter.measurements.GasAlertManager;
import com.example.mborper.breathbetter.measurements.LocationUtils;
import com.example.mborper.breathbetter.measurements.Measurement;
import com.example.mborper.breathbetter.measurements.NodeConnectionState;

/**
 * BeaconListeningService is an Android Service that scans for Bluetooth Low Energy (BLE) devices using
 * BluetoothLeScanner. It operates in the background as a foreground service and continuously scans for
 * devices based on a provided target UUID. When a matching device is found, it generates a Measurement object
 * with location data.
 *
 * @author Alejandro Rosado & Manuel Borregales
 * @since 2024-10-07
 * last edited: 2025-01-08
 */
public class BeaconListeningService extends Service {

    private static final String LOG_TAG = "BEACON_LISTENING_SERVICE";
    private static final int NOTIFICATION_ID = 1;
    private String targetDeviceUUID;

    private volatile boolean keepRunning = true;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private HandlerThread handlerThread;
    private Handler serviceHandler;
    private static final long SCAN_PERIOD = 1000;  // The time in milliseconds to scan for BLE devices
    private static final long SCAN_INTERVAL = 10000; // The interval between scans

    private final IBinder binder = new LocalBinder();
    private MeasurementCallback measurementCallback;
    private GasAlertManager gasAlertManager;
    private Measurement lastMeasurement;
    private LocationUtils locationUtils;
    private Location currentLocation; // Stores the most recent location update

    private boolean measurementSentInCycle = false;
    private NodeConnectionState connectionState;

    /**
     * Interface for callback when a new measurement is received.
     */
    public interface MeasurementCallback {
        void onMeasurementReceived(Measurement measurement);
    }

    /**
     * Manages the intervals between scanning and non scanning periods
     */
    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            measurementSentInCycle = false;
            startScan();

            serviceHandler.postDelayed(() -> {
                stopScan();
                if (keepRunning) {
                    serviceHandler.postDelayed(scanRunnable, SCAN_INTERVAL - SCAN_PERIOD);
                }
            }, SCAN_PERIOD);
        }
    };

    /**
     * LocalBinder is used to bind the service to components such as activities.
     */
    public class LocalBinder extends Binder {
        public BeaconListeningService getService() {
            return BeaconListeningService.this;
        }
    }

    /**
     * Provides a binder for the service to communicate with the client.
     * <p>
     * Intent -> onBind() -> IBinder
     *
     * @param intent The intent used to bind the service.
     * @return The IBinder instance for communication with the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Called when the service is created. Initializes necessary components for the service to function.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        gasAlertManager = new GasAlertManager(this);
        initializeHandlerThread();
        initializeBluetooth();
        startBackgroundService();
        initializeLocationUtils();
        this.connectionState = NodeConnectionState.getInstance();
    }

    /**
     * Initializes the LocationUtils component and sets up location updates listener.
     * When a location update is received, it stores the current location and updates
     * any pending measurements with the new location data.
     */
    private void initializeLocationUtils() {
        locationUtils = new LocationUtils(this);
        locationUtils.setLocationUpdateListener(location -> {
            currentLocation = location;
            Log.d(LOG_TAG, "Location retrieved from LocationUtils: " + location.getLatitude() + ", " + location.getLongitude());
        });
        locationUtils.startLocationUpdates();
    }

    /**
     * Updates a measurement with the current location data if available.
     * This ensures that each measurement has the most accurate location information.
     *
     * @param measurement The Measurement object to update with location data
     */
    private void updateMeasurementWithLocation(Measurement measurement) {
        if (currentLocation != null) {
            measurement.setLatitude(currentLocation.getLatitude());
            measurement.setLongitude(currentLocation.getLongitude());
        }
    }

    /**
     * Starts the service in the background with a notification, so it is less likely to be killed by the system
     * also the user can be aware when the service is active.
     */
    private void startBackgroundService() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                "BLE_CHANNEL_ID",
                "BLE Service Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        if (notificationManager.getNotificationChannel("BLE_CHANNEL_ID") == null) {
            notificationManager.createNotificationChannel(channel);
        }

        // Create a PendingIntent that opens the MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE // Mandatory on Android 12+
        );

        Notification notification = new NotificationCompat.Builder(this, "BLE_CHANNEL_ID")
                .setContentTitle("Howsair")
                .setContentText("Buscando medidas de tu nodo...")
                .setSmallIcon(R.drawable.howsair_logo)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setSilent(true)
                .setContentIntent(pendingIntent) // When notification is clicked, open MainActivity
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * Called when the service is started with a start command. It receives the target device UUID to scan for.
     *
     * @param intent The intent that starts the service.
     * @param flags Additional flags about the start request.
     * @param startId A unique identifier for this specific start request.
     * @return Integer indicating how the system should handle the service if it is killed.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "BeaconListeningService.onStartCommand: starts");
        if (intent != null) {
            targetDeviceUUID = intent.getStringExtra("targetDeviceUUID");
            keepRunning = true;
            serviceHandler.post(scanRunnable);
        }
        return START_NOT_STICKY;
    }

    /**
     * Initializes a background thread to handle the scanning of BLE devices.
     */
    private void initializeHandlerThread() {
        handlerThread = new HandlerThread("BeaconListeningServiceThread");
        handlerThread.start();
        serviceHandler = new Handler(handlerThread.getLooper());
    }

    /**
     * Initializes the Bluetooth scanner if Bluetooth is enabled on the device.
     * Also checks for required Bluetooth permissions.
     */
    public void initializeBluetooth() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            Log.e(LOG_TAG, "Device does not support Bluetooth");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "BLUETOOTH_CONNECT permission not granted");
        }

        if (bta.isEnabled()) {
            this.scanner = bta.getBluetoothLeScanner();
            Log.d(LOG_TAG, "Bluetooth scanner initialized");
        } else {
            Log.e(LOG_TAG, "Bluetooth is not enabled");
        }
    }

    /**
     * Continuously performs BLE device scans while the service is running.
     * then sends the result to processScanResult()
     */
    private void startScan() {
        if (scanner == null) {
            Log.e(LOG_TAG, "Scanner is null, attempting to initialize Bluetooth");
            initializeBluetooth();
            if (scanner == null) {
                Log.e(LOG_TAG, "Failed to initialize scanner");
                return;
            }
        }

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                processScanResult(result);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(LOG_TAG, "Scan failed with error code: " + errorCode);
            }
        };

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // empty body because on old android versions wouldnt work
            }
            scanner.startScan(null, new ScanSettings.Builder().build(), scanCallback);
            Log.d(LOG_TAG, "Scan started successfully");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error starting scan: " + e.getMessage());
        }
    }

    /**
     * Stops the BLE device scans when the service is stopped
     */
    private void stopScan() {
        if (scanner != null && scanCallback != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (locationUtils != null) {
                locationUtils.startLocationUpdates(); // force update
                Log.w(LOG_TAG, "The location updates have stopped, restarting location updates...");
            }

            try {
                scanner.stopScan(scanCallback);
                Log.d(LOG_TAG, "Scan stopped successfully");

                if (locationUtils != null) {
                    if (!locationUtils.isTrackingLocation()) {
                        Log.w(LOG_TAG, "The scan has stopped, restarting location updates...");
                        locationUtils.startLocationUpdates();
                    }
                }

                // Generate and send the measurement after scan
                if (currentLocation != null) {
                    Measurement measurement = new Measurement();
                    measurement.setLatitude(currentLocation.getLatitude());
                    measurement.setLongitude(currentLocation.getLongitude());

                    if (isValidMeasurement(measurement)) {
                        processMeasurement(measurement);
                    } else {
                        Log.w(LOG_TAG, "Medida inválida ignorada.");
                    }
                } else {
                    Log.w(LOG_TAG, "Ubicación no disponible para la medida.");
                }

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error stopping scan: " + e.getMessage());
            }
        }
    }

    /**
     * Processes the result of a BLE device scan. If a device with the matching UUID is found,
     * creates a new Measurement with location data and processes it if valid.
     *
     * @param result The result of the BLE scan containing device information.
     */
    private void processScanResult(ScanResult result) {
        if (measurementSentInCycle) {
            return;
        }

        IBeaconFrame tib = new IBeaconFrame(result.getScanRecord().getBytes());
        if (Utilities.bytesToString(tib.getUUID()).equals(targetDeviceUUID)) {
            Measurement newMeasurement = new Measurement();
            newMeasurement.setO3Value(Utilities.bytesToFloat(tib.getMajor()) / 100);
            updateMeasurementWithLocation(newMeasurement);

            if (isValidMeasurement(newMeasurement)) {
                if (!newMeasurement.equals(lastMeasurement)) {
                    processMeasurement(newMeasurement);
                    lastMeasurement = newMeasurement;
                }
            }
        }
    }

    /**
     * Validates that a measurement contains valid data within acceptable ranges.
     *
     * @param measurement The Measurement object to validate
     * @return boolean indicating if the measurement is valid
     */
    private boolean isValidMeasurement(Measurement measurement) {
        return measurement != null &&
                measurement.getO3Value() >= 0 &&
                measurement.getO3Value() <= 1000;
    }

    /**
     * Processes a valid measurement by updating connection state and notifying listeners.
     * Also triggers gas alerts if necessary.
     *
     * @param newMeasurement The new Measurement to process
     */
    private void processMeasurement(Measurement newMeasurement) {
        boolean isValidMeasurement = isValidMeasurement(newMeasurement);
        connectionState.updateConnectionState(isValidMeasurement);

        if (isValidMeasurement) {
            if (measurementCallback != null) {
                measurementCallback.onMeasurementReceived(newMeasurement);
            }

            gasAlertManager.onMeasurementReceived(newMeasurement.getO3Value());
        }
    }

    /**
     * Stops the service, cleaning up resources and removing notifications.
     */
    public void stopService() {
        Log.d(LOG_TAG, "stopService called");
        keepRunning = false;

        if (serviceHandler != null) {
            serviceHandler.removeCallbacks(scanRunnable);
        }

        if (gasAlertManager != null) {
            gasAlertManager.cleanup();
        }

        stopScan();
        stopForeground(true);
        stopSelf();
    }

    /**
     * Called when the service is destroyed. Cleans up all resources including location updates,
     * Bluetooth scanning, and background threads.
     */
    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy called");
        keepRunning = false;

        if (serviceHandler != null) {
            serviceHandler.removeCallbacks(scanRunnable);
        }
        stopScan();

        if (gasAlertManager != null) {
            gasAlertManager.cleanup();
        }

        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                handlerThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Error shutting down handler thread: " + e.getMessage());
            }
        }

        if (locationUtils != null) {
            locationUtils.stopLocationUpdates();
        }

        super.onDestroy();
    }

    /**
     * Sets the callback to receive new measurements.
     *
     * @param callback The callback to be invoked when a new measurement is available.
     */
    public void setMeasurementCallback(MeasurementCallback callback) {
        this.measurementCallback = callback;
    }

    /**
     * Returns the last measurement taken.
     *
     * @return The last Measurement object.
     */
    public Measurement getLastMeasurement() {
        return lastMeasurement;
    }
}
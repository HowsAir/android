package com.example.mborper.breathbetter.bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import com.example.mborper.breathbetter.measurements.GasAlertManager;
import com.example.mborper.breathbetter.measurements.LocationUtils;
import com.example.mborper.breathbetter.measurements.Measurement;

/**
 * BeaconListeningService is an Android Service that scans for Bluetooth Low Energy (BLE) devices using
 * BluetoothLeScanner. It operates in the background as a foreground service and continuously scans for
 * devices based on a provided target UUID. When a matching device is found, it generates a Measurement object.
 * @author Manuel Borregales
 * date: 2024-10-07
 * last edited: 2024-10-23
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
            if (!keepRunning) {
                return;
            }

            startScan();

            // Schedule scan stop after SCAN_PERIOD
            serviceHandler.postDelayed(() -> {
                stopScan();

                if (keepRunning) {
                    //Log.d(LOG_TAG, "Scheduling next scan cycle");
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
     * Called when the service is bound to a client.
     *
     * @param intent The intent that was used to bind to this service.
     * @return The IBinder interface that clients use to communicate with the service.
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
                NotificationManager.IMPORTANCE_LOW
        );
        notificationManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, "BLE_CHANNEL_ID")
                .setContentTitle("BLE Service")
                .setContentText("Scanning for BLE devices...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    /**
     * Called when the service is started with a start command. It receives the target device UUID to scan for.
     * <p>
     *          Intent
     *      Natural: flags  ---> onStartCommand() --->  Natural
     *      Natural: startId
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
                // empty body because on old android versions wouldnt work
            }

            try {
                scanner.stopScan(scanCallback);
                Log.d(LOG_TAG, "Scan stopped successfully");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error stopping scan: " + e.getMessage());
            }
        }
    }

    /**
     * Processes the result of a BLE device scan. If a device with the matching UUID obtained in onStartCommand()
     * is found, a new Measurement is created.
     * <p>
     *    ScanResult:result ---> processScanResult()
     *
     * @param result The result of the BLE scan containing device information.
     */
    private void processScanResult(ScanResult result) {
        IBeaconFrame tib = new IBeaconFrame(result.getScanRecord().getBytes());
        if (Utilities.bytesToString(tib.getUUID()).equals(targetDeviceUUID)) {
            // Get ppm an temperature
            Measurement newMeasurement = new Measurement();
            newMeasurement.setO3Value(Utilities.bytesToInt(tib.getMajor()));

            // Get location for the measurement
            LocationUtils locationUtils = new LocationUtils(this);
            Location currentLocation = locationUtils.getCurrentLocation();
            if (currentLocation != null) {
                newMeasurement.setLatitude(currentLocation.getLatitude());
                newMeasurement.setLongitude(currentLocation.getLongitude());
            }

            //the date is set on the server logic
            if (!newMeasurement.equals(lastMeasurement)) {
                lastMeasurement = newMeasurement;
                if (measurementCallback != null) {
                    measurementCallback.onMeasurementReceived(newMeasurement);
                }

                gasAlertManager.checkAndAlert(newMeasurement.getO3Value());

                Log.d("main", "ppm" + lastMeasurement.getO3Value());
                Log.d("main", "lat" + lastMeasurement.getLatitude());
                Log.d("main", "long" + lastMeasurement.getLongitude());
            }
        }
    }

    /**
     * Called when the activity wants to stop the service. Cleans up resources such as stopping the scan and terminating the handler thread.
     */
    public void stopService() {
        Log.d(LOG_TAG, "stopService called");
        keepRunning = false;

        // Remove any pending scan runnables
        if (serviceHandler != null) {
            serviceHandler.removeCallbacks(scanRunnable);
        }

        if (gasAlertManager != null) {
            gasAlertManager.cleanup();
        }

        // Stop current scan if active
        stopScan();

        // Stop foreground service and self
        stopForeground(true);
        stopSelf();
    }

    /**
     * Called when the service is destroyed. Cleans up resources such as stopping the scan and terminating the handler thread.
     */
    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy called");
        keepRunning = false;

        // Remove any pending scan runnables
        if (serviceHandler != null) {
            serviceHandler.removeCallbacks(scanRunnable);
        }
        stopScan();

        if (gasAlertManager != null) {
            gasAlertManager.cleanup();
        }

        // Clean up handler thread
        if (handlerThread != null) {
            handlerThread.quitSafely();
            try {
                handlerThread.join(1000); // Wait for thread to finish
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Error shutting down handler thread: " + e.getMessage());
            }
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
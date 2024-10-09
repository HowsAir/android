package com.example.mborper.breathbetter.bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
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
import com.example.mborper.breathbetter.api.Measurement;

import java.util.ArrayList;
import java.util.List;

/**
 * BeaconListeningService is an Android Service that scans for Bluetooth Low Energy (BLE) devices using
 * BluetoothLeScanner. It operates in the background as a foreground service and continuously scans for
 * devices based on a provided target UUID. When a matching device is found, it generates a Measurement object.
 *
 * @author  Manuel Borregales
 * @date:    2024-10-07
 */
public class BeaconListeningService extends Service {

    private static final String LOG_TAG = "BEACON_LISTENING_SERVICE";
    private String targetDeviceUUID;
    private boolean keepRunning = true;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    private static final int NOTIFICATION_ID = 1;
    private HandlerThread handlerThread;
    private Handler serviceHandler;
    private final IBinder binder = new LocalBinder();
    private MeasurementCallback measurementCallback;

    private static final long SCAN_PERIOD = 1000;  // The time in milliseconds to scan for BLE devices
    private static final long SCAN_INTERVAL = 10000; // The interval between scans
    private Measurement lastMeasurement;

    /**
     * Interface for callback when a new measurement is received.
     */
    public interface MeasurementCallback {
        void onMeasurementReceived(Measurement measurement);
    }

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
        Log.d(LOG_TAG, "BeaconListeningService: onCreate");
        startForegroundService();
        initializeHandlerThread();
        initializeBluetooth();
    }

    /**
     * Starts the service in the foreground with a notification, so it is less likely to be killed by the system
     * also the user can be aware when the service is active.
     */
    private void startForegroundService() {
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
        targetDeviceUUID = intent.getStringExtra("targetDeviceUUID");
        serviceHandler.post(() -> performWork());
        return START_STICKY;
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
    private void initializeBluetooth() {
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
     */
    private void performWork() {
        Log.d(LOG_TAG, "BeaconListeningService.performWork: starts");

        serviceHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!keepRunning) {
                    Log.d(LOG_TAG, "BeaconListeningService.performWork: stopping scan.");
                    stopBTLEDeviceSearch(); // Ensure to stop scanning
                    stopSelf(); // Stop the service completely
                    return;
                }

                if (scanner != null) {
                    Log.d(LOG_TAG, "Starting BLE scan.");
                    searchBTLEDevices();

                    // Stop scan after SCAN_PERIOD
                    serviceHandler.postDelayed(() -> {
                        Log.d(LOG_TAG, "Stopping BLE scan.");
                        stopBTLEDeviceSearch();

                        // Schedule the next scan after SCAN_INTERVAL if keepRunning is true
                        if (keepRunning) {
                            serviceHandler.postDelayed(this, SCAN_INTERVAL);
                        }
                    }, SCAN_PERIOD);
                } else {
                    Log.e(LOG_TAG, "Bluetooth scanner is null. Retrying in 10 seconds.");
                    serviceHandler.postDelayed(this, 10000);
                    initializeBluetooth();
                }
            }
        });
    }


    /**
     * Starts scanning for Bluetooth devices and sends the result to processScanResult()
     */
    private void searchBTLEDevices() {
        Log.d("SEARCH", "Starting search for all devices.");

        this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                processScanResult(result, targetDeviceUUID);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("SEARCH", "Scan failed with error code: " + errorCode);
            }
        };

        List<ScanFilter> filters = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "BLUETOOTH_SCAN permission not granted");
        }
        this.scanner.startScan(filters, new ScanSettings.Builder().build(), this.scanCallback);
    }

    /**
     * Processes the result of a BLE device scan. If a device with the matching UUID obtained in onStartCommand()
     * is found, a new Measurement is created.
     * <p>
     *    ScanResult:result ---> processScanResult()
     *
     * @param result The result of the BLE scan containing device information.
     */
    private void processScanResult(ScanResult result, String target) {
        IBeaconFrame tib = new IBeaconFrame(result.getScanRecord().getBytes());
        if (Utilities.bytesToString(tib.getUUID()).equals(target)) {
            Log.d("PROCESS", "Device UUID: " + Utilities.bytesToString(tib.getUUID()));
            Log.d("PROCESS", "Device Major: " + Utilities.bytesToInt(tib.getMajor()));
            Log.d("PROCESS", "Device Minor: " + Utilities.bytesToInt(tib.getMinor()));

            Measurement newMeasurement = new Measurement();
            newMeasurement.setPpm(Utilities.bytesToInt(tib.getMajor()));
            newMeasurement.setTemperature(Utilities.bytesToInt(tib.getMinor()));
            newMeasurement.setLatitude(50); // Replace with actual location data
            newMeasurement.setLongitude(50); // Replace with actual location data

            if (!newMeasurement.equals(lastMeasurement)) {
                lastMeasurement = newMeasurement;
                if (measurementCallback != null) {
                    measurementCallback.onMeasurementReceived(newMeasurement);
                }
            }
        }
    }

    /**
     * Stops the BLE device search by stopping the BLE scan.
     */
    private void stopBTLEDeviceSearch() {
        if (this.scanCallback != null && this.scanner != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.d(LOG_TAG, "STOPPING SCAN");
            keepRunning = false;
            stopSelf();
            this.scanner.stopScan(this.scanCallback);
            this.scanCallback = null;
        }
    }

    /**
     * Called when the service is destroyed. Cleans up resources such as stopping the scan and terminating the handler thread.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBTLEDeviceSearch();
        keepRunning = false;
        handlerThread.quitSafely();
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
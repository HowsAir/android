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


public class BeaconListeningService extends Service {

    private static final String LOG_TAG = "BEACON_LISTENING_SERVICE";
    private boolean keepRunning = true;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;

    private static final int NOTIFICATION_ID = 1;
    private HandlerThread handlerThread;
    private Handler serviceHandler;
    private final IBinder binder = new LocalBinder();
    private MeasurementCallback measurementCallback;

    private static final long SCAN_PERIOD = 3000;
    private static final long SCAN_INTERVAL = 10000;
    private Measurement lastMeasurement;

    public interface MeasurementCallback {
        void onMeasurementReceived(Measurement measurement);
    }

    public class LocalBinder extends Binder {
        public BeaconListeningService getService() {
            return BeaconListeningService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "BeaconListeningService: onCreate");
        startForegroundService();
        initializeHandlerThread();
        initializeBluetooth();
        //fakingMeasurements();
    }

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "BeaconListeningService.onStartCommand: starts");
        String targetDeviceUUID = intent.getStringExtra("targetDeviceUUID");
        serviceHandler.post(() -> performWork(targetDeviceUUID));
        return START_STICKY;
    }

    private void initializeHandlerThread() {
        handlerThread = new HandlerThread("BeaconListeningServiceThread");
        handlerThread.start();
        serviceHandler = new Handler(handlerThread.getLooper());
    }

    private void initializeBluetooth() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta == null) {
            Log.e(LOG_TAG, "Device does not support Bluetooth");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "BLUETOOTH_CONNECT permission not granted");
            //return;
        }

        if (bta.isEnabled()) {
            this.scanner = bta.getBluetoothLeScanner();
            Log.d(LOG_TAG, "Bluetooth scanner initialized");
        } else {
            Log.e(LOG_TAG, "Bluetooth is not enabled");
        }
    }

    private void performWork(String targetDeviceUUID) {
        Log.d(LOG_TAG, "BeaconListeningService.performWork: starts: thread=" + Thread.currentThread().getId());

        while (keepRunning) {
            if (scanner != null) {
                searchBTLEDevices(targetDeviceUUID);
                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                stopBTLEDeviceSearch();
                try {
                    Thread.sleep(SCAN_INTERVAL - SCAN_PERIOD);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                Log.e(LOG_TAG, "Bluetooth scanner is null. Retrying in 10 seconds.");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                initializeBluetooth();
            }
        }

        Log.d(LOG_TAG, "BeaconListeningService.performWork: ends");
    }

    private void searchBTLEDevices(String target) {
        Log.d("SEARCH", "Starting search for all devices.");

        this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                //Log.d("SEARCH", "Device found: " + result);
                processScanResult(result, target);
                // Aquí puedes acceder a otros datos del ScanRecord si es necesario
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("SEARCH", "Scan failed with error code: " + errorCode);
            }
        };

        List<ScanFilter> filters = new ArrayList<>(); // Lista vacía para escanear

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "BLUETOOTH_SCAN permission not granted");
            //return;
        }
        this.scanner.startScan(filters, new ScanSettings.Builder().build(), this.scanCallback);
    }

    private void processScanResult(ScanResult result, String target) {
        IBeaconFrame tib = new IBeaconFrame(result.getScanRecord().getBytes());
        if(Utilities.bytesToString(tib.getUUID()).equals(target)) {
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

    private void stopBTLEDeviceSearch() {
        if (this.scanCallback != null && this.scanner != null) {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                //return;
            }
            this.scanner.stopScan(this.scanCallback);
            this.scanCallback = null;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBTLEDeviceSearch();
        keepRunning = false;
        handlerThread.quitSafely();
    }

    public void setMeasurementCallback(MeasurementCallback callback) {
        this.measurementCallback = callback;
    }

    public Measurement getLastMeasurement() {
        return lastMeasurement;
    }

    private void fakingMeasurements() {
        Measurement measurement = new Measurement();
        measurement.setPpm(50);
        measurement.setTemperature(50);
        measurement.setLatitude(50);
        measurement.setLongitude(50);
        if(measurementCallback != null) {
            measurementCallback.onMeasurementReceived(measurement);
        }
        Log.d("FAKE", measurement.toString());
    }
}
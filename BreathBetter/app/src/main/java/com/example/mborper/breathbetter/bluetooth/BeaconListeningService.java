package com.example.mborper.breathbetter.bluetooth;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.Manifest;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.Measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class BeaconListeningService extends IntentService {
    private static final String LOG_TAG = "BEACON_LISTENING_SERVICE";
    private static final UUID TARGET_UUID = UUID.fromString("D744C889-0168-4FA7-B264-8B7EA5C0F6D6"); // Specific UUID
    private boolean keepRunning = true;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    public Measurement actualMeasurement;
    private static final int NOTIFICATION_ID = 1;

    public BeaconListeningService() {
        super("BeaconListeningServiceWorkerThread");
        Log.d(LOG_TAG, "BeaconListeningService: constructor ends");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
        initializeBluetooth();
    }

    // Este método crea la notificación que usaremos para el servicio en primer plano
    private void startForegroundService() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Crear el canal de notificación para Android O y superiores
            NotificationChannel channel = new NotificationChannel(
                    "BLE_CHANNEL_ID",
                    "BLE Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Crear una notificación simple para mantener el servicio en primer plano
        Notification notification = new NotificationCompat.Builder(this, "BLE_CHANNEL_ID")
                .setContentTitle("BLE Service")
                .setContentText("Scanning for BLE devices...")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Icono que se mostrará en la notificación
                .build();

        // Iniciar el servicio en primer plano con la notificación
        startForeground(NOTIFICATION_ID, notification);
    }

    private void initializeBluetooth() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        if (bta == null) {
            Log.e(LOG_TAG, "Device does not support Bluetooth");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "BLUETOOTH_CONNECT permission not granted");
            return;
        }
        Log.d(LOG_TAG, "BeaconListeningService: BTA NOT NULL");
        if (bta.isEnabled()) {
            this.scanner = bta.getBluetoothLeScanner();
            Log.d(LOG_TAG, "Bluetooth scanner initialized");
        } else {
            Log.e(LOG_TAG, "Bluetooth is not enabled");
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "BeaconListeningService.onHandleIntent: starts: thread=" + Thread.currentThread().getId());
        String targetDevice = intent.getStringExtra("targetDevice");

        if (TARGET_UUID != null) {
            searchBeaconsByUUID(TARGET_UUID);
        } else {
            searchSpecificBTLEDevice(targetDevice);
        }

        try {
            while (keepRunning) {
                Thread.sleep(5000); // Scan every 5 seconds
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        stopBTLEDeviceSearch();
        Log.d(LOG_TAG, "BeaconListeningService.onHandleIntent: ends");
    }

    private void searchSpecificBTLEDevice(final String targetDevice) {
        this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d(LOG_TAG, "searchSpecificBTLEDevice(): onScanResult()");
                processScanResult(result);
            }
        };

        ScanFilter filter = new ScanFilter.Builder().setDeviceName(targetDevice).build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        startScan(filters);
    }

    private void searchBeaconsByUUID(UUID targetUUID) {
        this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d(LOG_TAG, "searchBeaconsByUUID(): onScanResult()");
                processScanResult(result);
            }
        };

        // Create a filter for the specified UUID
        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(targetUUID)) // Set the UUID filter
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        startScan(filters);
    }

    private void startScan(List<ScanFilter> filters) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "BLUETOOTH_SCAN permission not granted");
            return;
        }
        this.scanner.startScan(filters, new ScanSettings.Builder().build(), this.scanCallback);
    }

    private void processScanResult(ScanResult result) {
        IBeaconFrame tib = new IBeaconFrame(result.getScanRecord().getBytes());

        actualMeasurement = new Measurement();
        actualMeasurement.setPpm(Utilities.bytesToInt(tib.getMajor()));
        actualMeasurement.setTemperature(Utilities.bytesToInt(tib.getMinor()));
        actualMeasurement.setLatitude(50);
        actualMeasurement.setLongitude(50);
    }

    private void stopBTLEDeviceSearch() {
        if (this.scanCallback == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "BLUETOOTH_SCAN permission not granted");
            return;
        }
        this.scanner.stopScan(this.scanCallback);
        this.scanCallback = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBTLEDeviceSearch();
        keepRunning = false;
    }

    public Measurement getActualMeasurement() {
        return actualMeasurement;
    }
}

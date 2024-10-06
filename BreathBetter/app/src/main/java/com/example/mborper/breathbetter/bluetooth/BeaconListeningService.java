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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.ParcelUuid;
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
import java.util.UUID;


public class BeaconListeningService extends Service {

    private static final String LOG_TAG = "BEACON_LISTENING_SERVICE";
    private boolean keepRunning = true;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    public Measurement actualMeasurement;
    private static final int NOTIFICATION_ID = 1;
    private HandlerThread handlerThread;
    private Handler serviceHandler;
    private final IBinder binder = new LocalBinder();

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

    private void startForegroundService() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "BLE_CHANNEL_ID",
                    "BLE Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, "BLE_CHANNEL_ID")
                .setContentTitle("BLE Service")
                .setContentText("Scanning for BLE devices...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void initializeBluetooth() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        if (bta == null) {
            Log.e(LOG_TAG, "Device does not support Bluetooth");
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

    private void performWork(String targetDeviceUUID) {
        Log.d(LOG_TAG, "BeaconListeningService.performWork: starts: thread=" + Thread.currentThread().getId());

        searchSpecificBTLEDevice(targetDeviceUUID);

        try {
            while (keepRunning) {
                Thread.sleep(5000); // Scan every 5 seconds
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        stopBTLEDeviceSearch();
        Log.d(LOG_TAG, "BeaconListeningService.performWork: ends");
    }

    private void searchSpecificBTLEDevice(final String targetDeviceUUID) {
        Log.d("SEARCH", "Iniciando búsqueda para UUID: " + targetDeviceUUID);

        this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d("SEARCH", "Dispositivo encontrado: " + result.getDevice().getAddress());
                processScanResult(result, targetDeviceUUID);
            }
        };

        // Crear el UUID del beacon desde el string
        UUID serviceUuid = UUID.fromString(targetDeviceUUID);

        // Convertir el UUID a un ParcelUuid
        ParcelUuid parcelServiceUuid = new ParcelUuid(serviceUuid);

        // Crear un filtro basado en el ParcelUuid del servicio
                ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(parcelServiceUuid)
                        .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        this.scanner.startScan(filters, settings, this.scanCallback);
    }

    private void fakingMeasurements() {
        actualMeasurement = new Measurement();
        actualMeasurement.setPpm(50);
        actualMeasurement.setTemperature(50);
        actualMeasurement.setLatitude(50);
        actualMeasurement.setLongitude(50);
        Log.d("FAKE", actualMeasurement.toString());
    }

    public Measurement getActualMeasurement() { return actualMeasurement; }

    private void processScanResult(ScanResult result, String targetDeviceUUID) {
        IBeaconFrame tib = new IBeaconFrame(result.getScanRecord().getBytes());
        Log.d("PROCESS", "TARGET ANTES DEL IF" + targetDeviceUUID);
        Log.d("PROCESS", "TIB EN STRING" + Utilities.bytesToString(tib.getUUID()));
        if (Utilities.bytesToString(tib.getUUID()).equals(targetDeviceUUID)) {
            Log.e("RESULT", "THIS IS PPM" + Utilities.bytesToInt(tib.getMajor()));
            Log.e("RESULT", "THIS IS TEMP" + Utilities.bytesToInt(tib.getMinor()));
            actualMeasurement = new Measurement();
            actualMeasurement.setPpm(Utilities.bytesToInt(tib.getMajor()));
            actualMeasurement.setTemperature(Utilities.bytesToInt(tib.getMinor()));
            actualMeasurement.setLatitude(50);
            actualMeasurement.setLongitude(50);
        }
    }

    private void stopBTLEDeviceSearch() {
        if (this.scanCallback == null) {
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
        handlerThread.quitSafely();
    }
}
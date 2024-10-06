package com.example.mborper.breathbetter.bluetooth;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
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
    private static final int NOTIFICATION_ID = 1;
    private HandlerThread handlerThread;
    private Handler serviceHandler;
    private final IBinder binder = new LocalBinder();
    private MeasurementCallback measurementCallback;

    private static final long SCAN_PERIOD = 10000;
    private static final long SCAN_INTERVAL = 11000;

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
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(LOG_TAG, "Unable to initialize BluetoothManager.");
            return;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(LOG_TAG, "Device does not support Bluetooth");
            return;
        }

        if (bluetoothAdapter.isEnabled()) {
            this.scanner = bluetoothAdapter.getBluetoothLeScanner();
            Log.d(LOG_TAG, "Bluetooth scanner initialized");
        } else {
            Log.e(LOG_TAG, "Bluetooth is not enabled");
        }
    }

    private void performWork(String targetDeviceUUID) {
        Log.d(LOG_TAG, "BeaconListeningService.performWork: starts: thread=" + Thread.currentThread().getId());

        while (keepRunning) {
            if (scanner != null) {
                searchAllBTLEDevices();
                //searchSpecificBTLEDevice(targetDeviceUUID);
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

    private void searchAllBTLEDevices() {
        Log.d("SEARCH", "Starting search for all devices.");

        this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d("SEARCH", "Device found: " + result.getDevice().getAddress());
                // Aquí puedes acceder a otros datos del ScanRecord si es necesario
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("SEARCH", "Scan failed with error code: " + errorCode);
            }
        };

        List<ScanFilter> filters = new ArrayList<>(); // Lista vacía para escanear todo

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        scanner.startScan(filters, settings, this.scanCallback);

        /*this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d("SEARCH", "Device found: " + result.getDevice().getAddress());
                // Procesar todos los dispositivos encontrados
                processScanResult(result);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("SEARCH", "Scan failed with error code: " + errorCode);
            }
        };

        // No se utiliza filtro, por lo que se escaneará cualquier beacon
        List<ScanFilter> filters = new ArrayList<>(); // Lista vacía para escanear todo

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        scanner.startScan(filters, settings, this.scanCallback);*/
    }

    private void processScanResult(ScanResult result) {
        IBeaconFrame tib = new IBeaconFrame(result.getScanRecord().getBytes());

        // Aquí puedes registrar el UUID y otros datos del beacon
        Log.d("PROCESS", "Device UUID: " + Utilities.bytesToString(tib.getUUID()));
        Log.d("PROCESS", "Device Major: " + Utilities.bytesToInt(tib.getMajor()));
        Log.d("PROCESS", "Device Minor: " + Utilities.bytesToInt(tib.getMinor()));
    }


    /*private void searchSpecificBTLEDevice(final String targetDeviceUUID) {
        Log.d("SEARCH", "Starting search for UUID: " + targetDeviceUUID);

        this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d("SEARCH", "Device found: " + result.getDevice().getAddress());
                processScanResult(result, targetDeviceUUID);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e("SEARCH", "Scan failed with error code: " + errorCode);
            }
        };

        UUID serviceUuid = UUID.fromString(targetDeviceUUID);
        ParcelUuid parcelServiceUuid = new ParcelUuid(serviceUuid);

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(parcelServiceUuid)
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        scanner.startScan(filters, settings, this.scanCallback);
    }

    private void processScanResult(ScanResult result, String targetDeviceUUID) {
        IBeaconFrame tib = new IBeaconFrame(result.getScanRecord().getBytes());
        Log.d("PROCESS", "TARGET ANTES DEL IF" + targetDeviceUUID);
        Log.d("PROCESS", "TIB EN STRING" + Utilities.bytesToString(tib.getUUID()));
        if (Utilities.bytesToString(tib.getUUID()).equals(targetDeviceUUID)) {
            Log.e("RESULT", "THIS IS PPM" + Utilities.bytesToInt(tib.getMajor()));
            Log.e("RESULT", "THIS IS TEMP" + Utilities.bytesToInt(tib.getMinor()));
            Measurement measurement = new Measurement();
            measurement.setPpm(Utilities.bytesToInt(tib.getMajor()));
            measurement.setTemperature(Utilities.bytesToInt(tib.getMinor()));
            measurement.setLatitude(50); // Replace with actual location data
            measurement.setLongitude(50); // Replace with actual location data

            if (measurementCallback != null) {
                measurementCallback.onMeasurementReceived(measurement);
            }
        }
    }*/

    private void stopBTLEDeviceSearch() {
        if (this.scanCallback != null && this.scanner != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
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
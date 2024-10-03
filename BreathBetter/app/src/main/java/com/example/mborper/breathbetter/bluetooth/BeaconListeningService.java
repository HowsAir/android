package com.example.mborper.breathbetter.bluetooth;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.util.Log;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import android.Manifest;
import com.example.mborper.breathbetter.api.Measurement;

import java.util.ArrayList;
import java.util.List;


public class BeaconListeningService extends IntentService {
    private static final String LOG_TAG = ">>>>";
    private boolean keepRunning = true;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    public Measurement actualMeasurement;

    public BeaconListeningService() {
        super("BeaconListeningServiceWorkerThread");
        Log.d(LOG_TAG, "BeaconListeningService: constructor ends");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeBluetooth();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "BeaconListeningService.onHandleIntent: starts: thread=" + Thread.currentThread().getId());
        String targetDevice = intent.getStringExtra("targetDevice");
        searchSpecificBTLEDevice(targetDevice);

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



    private void initializeBluetooth() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "BLUETOOTH_CONNECT permission not granted");
            return;
        }
        bta.enable();
        this.scanner = bta.getBluetoothLeScanner();
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

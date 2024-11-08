package com.example.mborper.breathbetter;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * This class controls the buzzer on the Arduino by sending commands via Bluetooth.
 *
 * @author Alejandro Rosado
 * @since 2024-11-08
 */
public class BuzzerControl {
    private static final String LOG_TAG = "BuzzerControl";
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;

    private final Activity activity;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private Callback callback;

    public interface Callback {
        void onPermissionDenied();
        void onBluetoothNotSupported();
        void onSuccess();
        void onFailure(String error);
    }

    public BuzzerControl(Activity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            this.advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADVERTISE)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.BLUETOOTH_ADVERTISE},
                    BLUETOOTH_PERMISSION_REQUEST_CODE
            );
        } else {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.BLUETOOTH},
                    BLUETOOTH_PERMISSION_REQUEST_CODE
            );
        }
    }


    public void turnOnBuzzer() {
        if (advertiser == null) {
            Log.e(LOG_TAG, "BLE advertising not supported");
            callback.onBluetoothNotSupported();
            return;
        }

        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        // Make sure Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(LOG_TAG, "Bluetooth is not enabled");
            callback.onFailure("Bluetooth is not enabled");
            return;
        }

        try {
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setConnectable(false)
                    .setTimeout(1000) // Advertise for 1 second
                    .build();

            // Debug log the manufacturer data being sent
            byte[] manufacturerData = new byte[]{
                    (byte)0xBE, (byte)0xEE, // Identifier
                    0x01  // Command to activate
            };

            Log.d(LOG_TAG, "Sending manufacturer data: " +
                    String.format("0x%02X 0x%02X 0x%02X",
                            manufacturerData[0],
                            manufacturerData[1],
                            manufacturerData[2]));

            AdvertiseData data = new AdvertiseData.Builder()
                    .addManufacturerData(0x0000, manufacturerData)
                    .build();

            Log.d(LOG_TAG, "Starting BLE advertisement...");
            advertiser.startAdvertising(settings, data, advertiseCallback);

        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Security exception when starting advertising: " + e.getMessage());
            callback.onFailure("Permission denied: " + e.getMessage());
        }
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(LOG_TAG, "BLE Advertise Started Successfully");
            callback.onSuccess();
        }

        @Override
        public void onStartFailure(int errorCode) {
            String errorMessage = "Advertisement failed with error code: " + errorCode;
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    errorMessage = "Advertisement already started";
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    errorMessage = "Advertisement data too large";
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    errorMessage = "Advertisement feature unsupported";
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    errorMessage = "Internal advertising error";
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    errorMessage = "Too many advertisers";
                    break;
            }
            Log.e(LOG_TAG, errorMessage);
            callback.onFailure(errorMessage);
        }
    };

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                turnOnBuzzer();
            } else {
                callback.onPermissionDenied();
            }
        }
    }
}
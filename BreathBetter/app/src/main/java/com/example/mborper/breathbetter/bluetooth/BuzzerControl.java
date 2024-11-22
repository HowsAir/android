package com.example.mborper.breathbetter.bluetooth;

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
 * This class controls the buzzer on the Arduino by sending commands via Bluetooth Low Energy (BLE).
 * <p>
 * It handles permissions and Bluetooth status to ensure BLE advertisements
 * are properly configured and sent to trigger the buzzer.
 *
 * @author Alejandro Rosado
 * @since 2024-11-08
 * last updated 2024-11-10
 */
public class BuzzerControl {
    private static final String LOG_TAG = "BuzzerControl";
    private static final byte BUZZER_ON = 0x01;
    private static final byte BUZZER_OFF = 0x00;

    private final Activity activity;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private Callback callback;

    /**
     * Callback interface to handle Bluetooth control events.
     */
    public interface Callback {
        void onPermissionDenied();
        void onBluetoothNotSupported();
        void onSuccess();
        void onFailure(String error);
    }

    /**
     * Constructor that initializes BluetoothAdapter and BluetoothLeAdvertiser if available.
     *
     * @param activity the parent activity for context
     * @param callback the callback instance to handle Bluetooth control responses
     */
    public BuzzerControl(Activity activity, Callback callback) {
        this.activity = activity;
        this.callback = callback;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            this.advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        }
    }

    /**
     * Sends the BLE advertisement to activate the buzzer on the Arduino.
     * <p>
     * Checks if the device supports BLE advertising and if Bluetooth is enabled,
     * then constructs the advertising settings and manufacturer data with the activation command.
     */
    public void turnOnBuzzer() {
        sendBuzzerCommand(BUZZER_ON);
    }

    /**
     * Sends the BLE advertisement to deactivate the buzzer on the Arduino.
     * <p>
     * Configures advertisement settings and manufacturer data with the deactivation command,
     * and starts the BLE advertising to stop the buzzer.
     */
    public void turnOffBuzzer() {
        sendBuzzerCommand(BUZZER_OFF);
    }

    /**
     * Sends a command to control the buzzer via BLE advertisement.
     * <p>
     * This method handles the BLE advertisement process to send commands to the Arduino device.
     * It performs the following checks before sending:
     * <ul>
     *   <li> Verifies BLE advertising support
     *   <li> Checks for necessary Bluetooth permissions
     *   <li> Confirms Bluetooth is enabled
     * </ul>
     * The method constructs and sends a manufacturer-specific data packet with the format:
     * [0xBE, 0xEE, command], where command is either 0x00 (OFF) or 0x01 (ON).
     *
     * @param command the buzzer command to send (BUZZER_ON or BUZZER_OFF)
     */
    private void sendBuzzerCommand(byte command) {
        if (advertiser == null) {
            callback.onBluetoothNotSupported();
            return;
        }

        if (!BluetoothPermissionHandler.checkAndRequestBluetoothPermissions(activity)) {
            callback.onPermissionDenied();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            callback.onFailure("Bluetooth is not enabled");
            return;
        }

        try {
            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setConnectable(false)
                    .setTimeout(1000)
                    .build();

            byte[] manufacturerData = new byte[]{
                    (byte)0xBE, (byte)0xEE, // Identifier
                    command  // Command to activate/deactivate
            };

            AdvertiseData data = new AdvertiseData.Builder()
                    .addManufacturerData(0x0000, manufacturerData)
                    .build();

            advertiser.startAdvertising(settings, data, advertiseCallback);

        } catch (SecurityException e) {
            callback.onFailure("Permission denied: " + e.getMessage());
        }
    }


    /**
     * Callback to handle the result of the BLE advertisement start operation.
     */
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
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
            callback.onFailure(errorMessage);
        }
    };
}

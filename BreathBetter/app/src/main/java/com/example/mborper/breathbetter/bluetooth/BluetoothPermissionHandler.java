package com.example.mborper.breathbetter.bluetooth;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Manuel Borregales
 * Date: 06/10/2024
 *
 * The BluetoothPermissionHandler class handles the request and validation of Bluetooth-related permissions
 * in Android. It checks if the necessary permissions are granted, requests them if not, and processes
 * the result of the permission requests.
 */
public class BluetoothPermissionHandler {

    // Constant to identify the Bluetooth permissions request
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    /**
     * Checks and requests the necessary Bluetooth permissions. For Android 12 and above, it requests
     * BLUETOOTH_CONNECT and BLUETOOTH_SCAN permissions. For older versions, it requests BLUETOOTH and
     * BLUETOOTH_ADMIN. Additionally, it always requests location permission for scanning.
     *
     * @param activity The current activity where the permissions are being requested.
     * @return true if all required permissions are already granted, false if any permissions are requested.
     */
    public static boolean checkAndRequestBluetoothPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();

        // Check Bluetooth permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) and above
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            // Android 11 (API 30) and below
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }

        // Location permission is required for Bluetooth scanning on all Android versions
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Request any missing permissions
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(activity, permissions.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
            return false; // Permissions were requested
        }

        return true; // All permissions are already granted
    }

    /**
     * Handles the result of a permission request. If any of the requested permissions are denied,
     * this method returns false.
     *
     * @param requestCode The code identifying the permission request.
     * @param grantResults The results of the requested permissions.
     * @return true if all permissions were granted, false if any were denied.
     */
    public static boolean handlePermissionResult(int requestCode, int[] grantResults) {
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}

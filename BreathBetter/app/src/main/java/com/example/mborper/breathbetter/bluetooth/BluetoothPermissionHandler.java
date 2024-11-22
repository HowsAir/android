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
 * The BluetoothPermissionHandler class handles the request and validation of Bluetooth-related permissions
 * in Android. It checks if the necessary permissions are granted, requests them if not, and processes
 * the result of the permission requests.
 *
 * @author Manuel Borregales
 * @since 2024-10-06
 * last updated 2024-11-22
 */
public class BluetoothPermissionHandler {

    // Constant to identify the Bluetooth permissions request
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    /**
     * Checks and requests the necessary Bluetooth permissions. For Android 12 and above, it requests
     * BLUETOOTH_CONNECT, BLUETOOTH_SCAN and BLUETOOTH_ADVERTISE permissions. For older versions, it requests BLUETOOTH and
     * BLUETOOTH_ADMIN. Additionally, it always requests location permission for scanning.
     *
     * @param activity The current activity where the permissions are being requested.
     * @return true if all required permissions are already granted, false if any permissions are requested.
     */
    public static boolean checkAndRequestBluetoothPermissions(Activity activity) {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) and above
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADVERTISE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        } else {
            // Android 11 (API 30) and below
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
        }

        // Location permission is required for Bluetooth scanning on all Android versions
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
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
            // All permissions must be granted
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

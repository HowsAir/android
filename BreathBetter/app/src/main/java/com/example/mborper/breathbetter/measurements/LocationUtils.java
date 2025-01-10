package com.example.mborper.breathbetter.measurements;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Utility class for handling location-related operations, including starting and stopping
 * location updates and managing the current location state.
 *
 * @author Manuel Borregales
 * @author Alejandro Rosado
 * @since  2024-10-24
 * last edited: 2025-01-10
 */
public class LocationUtils {
    private static final String TAG = "LocationUtils";
    // Reduced intervals for more frequent updates
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final float MIN_DISTANCE_CHANGE = 5f; // 5 meters

    private static final float MAX_ACCURACY_THRESHOLD = 10.0f; // 10 meters of maximum accuracy acceptable
    private static final double SIGNIFICANT_CHANGE_THRESHOLD = 0.0001; // Approx 10 meters in coordinates


    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private LocationUpdateListener locationUpdateListener;
    private PowerManager.WakeLock wakeLock;
    private boolean isTracking = false;

    public interface LocationUpdateListener {
        void onLocationUpdated(Location location);
    }

    /**
     * Constructor to initialize the LocationUtils class with required context and services.
     * <p>
     * Flow: Context -> LocationServices -> PowerManager -> setupLocationCallback()
     *
     * @param context The application context used to initialize location services and PowerManager.
     */
    public LocationUtils(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BreathBetter:LocationWakeLock");
        setupLocationCallback();
    }

    /**
     * Sets the listener to receive location updates.
     *
     * @param listener An implementation of LocationUpdateListener to handle location updates.
     */
    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationUpdateListener = listener;
    }

    /**
     * Sets up the callback to handle location updates and forwards them to the listener.
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w(TAG, "Location result is null");
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    // Check for accuracy and whether the change is significant
                    if (isLocationAcceptable(location)) {
                        currentLocation = location;
                        if (locationUpdateListener != null) {
                            locationUpdateListener.onLocationUpdated(location);
                        }
                        Log.d(TAG, String.format("Location updated - Lat: %.7f, Lon: %.7f, Accuracy: %.2fm",
                                location.getLatitude(), location.getLongitude(), location.getAccuracy()));
                    } else {
                        Log.d(TAG, String.format("Location descartada - Precisión: %.2fm o cambio insignificante",
                                location.getAccuracy()));
                    }
                } else {
                    Log.w(TAG, "Ubicación recibida es null.");
                }
            }
        };
    }

    /**
     * Checks if the new location meets the quality and significant change criteria.
     * @param newLocation The new location to check
     * @return true if the location is acceptable, false otherwise
     */
    private boolean isLocationAcceptable(Location newLocation) {
        // Verify precision and significant change
        if (newLocation.getAccuracy() > MAX_ACCURACY_THRESHOLD) {
            return false;
        }

        // If there is no previous location, accept the new one if it has good accuracy
        if (currentLocation == null) {
            return true;
        }

        // Calculate if the change is significant
        double latDiff = Math.abs(newLocation.getLatitude() - currentLocation.getLatitude());
        double lonDiff = Math.abs(newLocation.getLongitude() - currentLocation.getLongitude());

        return latDiff > SIGNIFICANT_CHANGE_THRESHOLD || lonDiff > SIGNIFICANT_CHANGE_THRESHOLD;
    }

    /**
     * Starts requesting location updates using the FusedLocationProviderClient.
     * <p>
     * Flow: WakeLock -> check permissions -> LocationRequest -> requestLocationUpdates()
     */
    public void startLocationUpdates() {

        if (!wakeLock.isHeld()) {
            wakeLock.acquire(24 * 60 * 60 * 1000L); // 24 h
            Log.d(TAG, "WakeLock acquired");
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permissions not granted");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE)
                .setMaxUpdateDelayMillis(UPDATE_INTERVAL)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());

            // Get immediate location with an accuracy check
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        if (location != null && isLocationAcceptable(location)) {
                            currentLocation = location;
                            if (locationUpdateListener != null) {
                                locationUpdateListener.onLocationUpdated(location);
                            }
                            Log.d(TAG, String.format("Ubicación inicial - Lat: %.7f, Lon: %.7f, Precisión: %.2fm",
                                    location.getLatitude(), location.getLongitude(), location.getAccuracy()));
                        }
                    });

            isTracking = true;
            Log.d(TAG, "Actualizaciones de ubicación iniciadas");
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar actualizaciones de ubicación: " + e.getMessage());
        }
    }

    /**
     * Stops requesting location updates and releases the WakeLock if held.
     */
    public void stopLocationUpdates() {
        if (isTracking) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "WakeLock released");
            }

            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                Log.d(TAG, "Location updates stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping location updates: " + e.getMessage());
            }
        }
    }

    /**
     * Retrieves the most recent location if available.
     *
     * @return The current Location object, or null if not available.
     */
    public Location getCurrentLocation() {
        return currentLocation;
    }

    /**
     * Retrieves the current latitude from the last known location.
     *
     * @return The latitude as a double, or 0.0 if the location is unavailable.
     */
    public double getCurrentLatitude() {
        return currentLocation != null ? currentLocation.getLatitude() : 0.0;
    }

    /**
     * Retrieves the current longitude from the last known location.
     *
     * @return The longitude as a double, or 0.0 if the location is unavailable.
     */
    public double getCurrentLongitude() {
        return currentLocation != null ? currentLocation.getLongitude() : 0.0;
    }

    /**
     * Checks if the location tracking is active.
     *
     * @return true if location tracking is active, false otherwise.
     */
    public boolean isTrackingLocation() {
        return isTracking;
    }

}
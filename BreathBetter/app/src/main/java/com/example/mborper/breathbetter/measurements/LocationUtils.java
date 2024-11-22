package com.example.mborper.breathbetter.measurements;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationUtils {
    private static final String TAG = "LocationUtils";
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds
    private static final long FASTEST_UPDATE_INTERVAL = 5000; // 5 seconds

    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private LocationUpdateListener locationUpdateListener;

    public interface LocationUpdateListener {
        void onLocationUpdated(Location location);
    }

    public LocationUtils(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        setupLocationCallback();
    }

    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationUpdateListener = listener;
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w(TAG, "Location result is null");
                    return;
                }

                currentLocation = locationResult.getLastLocation();
                if (currentLocation != null && locationUpdateListener != null) {
                    locationUpdateListener.onLocationUpdated(currentLocation);
                }
            }
        };
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
            Log.d(TAG, "Location updates started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting location updates: " + e.getMessage());
        }
    }

    public void stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location updates: " + e.getMessage());
        }
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public double getCurrentLatitude() {
        return currentLocation != null ? currentLocation.getLatitude() : 0.0;
    }

    public double getCurrentLongitude() {
        return currentLocation != null ? currentLocation.getLongitude() : 0.0;
    }
}
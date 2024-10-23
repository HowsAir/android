package com.example.mborper.breathbetter.measurements;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import android.util.Log;

public class LocationUtils {
    private static final String LOG_TAG = "LocationUtils";
    private final Context context;
    private final LocationManager locationManager;

    public LocationUtils(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public Location getCurrentLocation() {
        if (locationManager == null) {
            Log.e(LOG_TAG, "LocationManager is null");
            return null;
        }

        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(LOG_TAG, "Location permission not granted");
                return null;
            }

            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gpsLocation != null) {
                return gpsLocation;
            }

            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return networkLocation;

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error getting location: " + e.getMessage());
            return null;
        }
    }

    public String getLocationString(Location location) {
        if (location == null) {
            return "Location not available";
        }
        return String.format(
                "Lat: %.6f, Long: %.6f",
                location.getLatitude(),
                location.getLongitude()
        );
    }
}
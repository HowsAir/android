package com.example.mborper.breathbetter.measurements;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import android.util.Log;

/**
 * LocationUtils
 * <p>
 * Utility class that handles location services in the app. It provides methods
 * to retrieve the user's current location using GPS or network, and formats
 * the location data into a human-readable string. Ensures that location
 * permissions are checked before attempting to access the device's location.
 *
 * @author Manuel Borregales
 * date:  2024-10-23
 */

public class LocationUtils {
    // Tag used for logging errors and important events related to location services
    private static final String LOG_TAG = "LocationUtils";

    // Application context and LocationManager to interact with location services
    private final Context context;
    private final LocationManager locationManager;

    /**
     * Constructor for LocationUtils
     *
     * @param context Application context used to get the LocationManager service.
     */
    public LocationUtils(Context context) {
        this.context = context;
        // Retrieves the system's location service
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Retrieves the current location of the device.
     * <p>
     * This method first checks if the LocationManager is available and if the
     * location permissions have been granted. If permissions are missing or an
     * error occurs, it logs an error and returns null. The location is retrieved
     * using either GPS or the network provider, depending on availability.
     *
     * @return the current Location object, or null if unavailable.
     */
    public Location getCurrentLocation() {
        if (locationManager == null) {
            // Logs an error if LocationManager is not initialized
            Log.e(LOG_TAG, "LocationManager is null");
            return null;
        }

        try {
            // Checks if the app has permission to access fine location
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(LOG_TAG, "Location permission not granted");
                return null;
            }

            // Attempts to retrieve the last known GPS location
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (gpsLocation != null) {
                return gpsLocation;
            }

            // Attempts to retrieve the last known network-based location
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return networkLocation;

        } catch (Exception e) {
            // Logs any exceptions that occur while fetching the location
            Log.e(LOG_TAG, "Error getting location: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a Location object into a readable string format.
     * <p>
     * If the location is null, it returns "Location not available". Otherwise,
     * it returns a formatted string with latitude and longitude values.
     *
     * @param location Location object containing latitude and longitude.
     * @return A string representation of the location, or a message if unavailable.
     */
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

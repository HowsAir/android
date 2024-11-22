package com.example.mborper.breathbetter.measurements;

import java.io.Serializable;

/**
 * The Measurement class represents the data model for a measurement object.
 * It implements Serializable so that it can be easily passed between activities or services in Android.
 *
 * @author  Manuel Borregales
 * @since    04/10/2024
 */
public class Measurement implements Serializable {

    private int o3Value;
    private double latitude;
    private double longitude;

    /**
     * Returns the O3 Value (parts per million) value of the measurement.
     * <p>
     *      getO3Value() ---> Natural:o3Value
     *
     * @return o3Value as a double.
     */
    public int getO3Value() {
        return o3Value;
    }

    /**
     * Sets the o3Value (parts per million) value for the measurement.
     * <p>
     *      Natural:o3Value ---> setO3Value()
     *
     * @param o3Value The o3Value to be set.
     */
    public void setO3Value(int o3Value) {
        this.o3Value = o3Value;
    }

    /**
     * Returns the latitude where the measurement was taken.
     *<p>
     *      getLatitude() ---> Real:latitude
     *
     * @return latitude as a double.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the latitude value for the measurement.
     *<p>
     *      Real:latitude ---> setLatitude()
     *
     * @param latitude The latitude value to be set.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Returns the longitude where the measurement was taken.
     *<p>
     *      getLongitude() ---> Real:longitude
     *
     * @return longitude as a double.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the longitude value for the measurement.
     *<p>
     *      Real:longitude ---> setLongitude()
     *
     * @param longitude The longitude value to be set.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
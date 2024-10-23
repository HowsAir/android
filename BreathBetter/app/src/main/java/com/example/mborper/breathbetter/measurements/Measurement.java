package com.example.mborper.breathbetter.measurements;

import java.io.Serializable;

/**
 * The Measurement class represents the data model for a measurement object.
 * It implements Serializable so that it can be easily passed between activities or services in Android.
 *
 * @author  Manuel Borregales
 * @date:    04/10/2024
 */
public class Measurement implements Serializable {

    private int ppm;
    private int temperature;
    private double latitude;
    private double longitude;

    /**
     * Returns the PPM (parts per million) value of the measurement.
     * <p>
     *      getPpm() ---> Natural:ppm
     *
     * @return ppm value as a double.
     */
    public int getPpm() {
        return ppm;
    }

    /**
     * Sets the PPM (parts per million) value for the measurement.
     * <p>
     *      Natural:Ppm ---> setPpm()
     *
     * @param ppm The PPM value to be set.
     */
    public void setPpm(int ppm) {
        this.ppm = ppm;
    }

    /**
     * Returns the temperature value of the measurement.
     *<p>
     *      getTemperature() ---> Natural:temperature
     *
     * @return temperature as a double.
     */
    public int getTemperature() {
        return temperature;
    }

    /**
     * Sets the temperature value for the measurement.
     *<p>
     *      Natural:temperature ---> setTemperature()
     *
     * @param temperature The temperature value to be set.
     */
    public void setTemperature(int temperature) {
        this.temperature = temperature;
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
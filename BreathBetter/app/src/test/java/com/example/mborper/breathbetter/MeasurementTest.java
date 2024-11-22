package com.example.mborper.breathbetter;

// MeasurementTest.java
import static org.junit.Assert.assertEquals;

import com.example.mborper.breathbetter.measurements.Measurement;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the Measurement class.
 * <p>
 * This class includes tests for setting and getting values of
 * O3, latitude, and longitude in a Measurement instance.
 *
 * @author Manuel Borregales
 * @since 2024-10-08
 * last edited: 2024-11-08
 */
public class MeasurementTest {

    private Measurement measurement;

    /**
     * Sets up a new instance of Measurement before each test.
     */
    @Before
    public void setUp() {
        measurement = new Measurement();
    }

    /**
     * Tests the setO3Value and getO3Value methods.
     * <p>
     * Verifies if a set value for O3 (ppm) can be accurately retrieved.
     */
    @Test
    public void testSetAndGetPpm() {
        int ppm = 500;
        measurement.setO3Value(ppm);
        assertEquals(ppm, measurement.getO3Value(), 0.001);
    }

    /**
     * Tests the setLatitude and getLatitude methods.
     * <p>
     * Verifies if a set latitude value can be accurately retrieved.
     */
    @Test
    public void testSetAndGetLatitude() {
        double latitude = 40.7128;
        measurement.setLatitude(latitude);
        assertEquals(latitude, measurement.getLatitude(), 0.001);
    }

    /**
     * Tests the setLongitude and getLongitude methods.
     * <p>
     * Verifies if a set longitude value can be accurately retrieved.
     */
    @Test
    public void testSetAndGetLongitude() {
        double longitude = -74.0060;
        measurement.setLongitude(longitude);
        assertEquals(longitude, measurement.getLongitude(), 0.001);
    }
}


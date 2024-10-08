package com.example.mborper.breathbetter;

// MeasurementTest.java
import static org.junit.Assert.assertEquals;

import com.example.mborper.breathbetter.api.Measurement;

import org.junit.Before;
import org.junit.Test;

public class MeasurementTest {

    private Measurement measurement;

    @Before
    public void setUp() {
        measurement = new Measurement();
    }

    @Test
    public void testSetAndGetPpm() {
        double ppm = 500.0;
        measurement.setPpm(ppm);
        assertEquals(ppm, measurement.getPpm(), 0.001);
    }

    @Test
    public void testSetAndGetTemperature() {
        double temperature = 25.5;
        measurement.setTemperature(temperature);
        assertEquals(temperature, measurement.getTemperature(), 0.001);
    }

    @Test
    public void testSetAndGetLatitude() {
        double latitude = 40.7128;
        measurement.setLatitude(latitude);
        assertEquals(latitude, measurement.getLatitude(), 0.001);
    }

    @Test
    public void testSetAndGetLongitude() {
        double longitude = -74.0060;
        measurement.setLongitude(longitude);
        assertEquals(longitude, measurement.getLongitude(), 0.001);
    }
}


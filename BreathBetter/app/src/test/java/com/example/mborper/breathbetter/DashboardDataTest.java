package com.example.mborper.breathbetter;

import com.example.mborper.breathbetter.api.ApiService;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import org.junit.Before;
import org.junit.Test;

import retrofit2.Call;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for dashboard data retrieval functionality
 * Utilizes the ApiService interface to mock API calls and verify dashboard data retrieval.
 *
 * @author Alejandro Rosado
 * @since 2024-12-12
 */
public class DashboardDataTest {

    private ApiService mockApiService;

    /**
     * Sets up mock API service before each test
     * Creates a mock instance of ApiService to simulate API interactions for testing.
     */
    @Before
    public void setUp() {
        mockApiService = mock(ApiService.class);
    }

    /**
     * Tests successful retrieval of dashboard data
     * @throws Exception if an error occurs during API interaction
     *
     * Call<JsonObject> -> getDashboardData() -> Response<JsonObject>
     *
     * Verifies that the dashboard data response is successful and contains expected data.
     */
    @Test
    public void testGetDashboardData_success() throws Exception {
        // Prepare test data
        JsonObject fakeJsonObject = new JsonObject();

        // Last air quality reading
        JsonObject lastAirQuality = new JsonObject();
        lastAirQuality.addProperty("aqi", 45);
        lastAirQuality.addProperty("category", "Good");
        fakeJsonObject.add("lastAirQuality", lastAirQuality);

        // Today's distance
        fakeJsonObject.addProperty("todayDistance", 1250.5);

        // Historical air quality readings
        JsonArray historicalReadings = new JsonArray();
        JsonObject reading1 = new JsonObject();
        reading1.addProperty("date", "2024-12-10");
        reading1.addProperty("aqi", 40);
        JsonObject reading2 = new JsonObject();
        reading2.addProperty("date", "2024-12-11");
        reading2.addProperty("aqi", 50);
        historicalReadings.add(reading1);
        historicalReadings.add(reading2);
        fakeJsonObject.add("historicalAirQuality", historicalReadings);

        // Create mock response
        Response<JsonObject> fakeResponse = Response.success(fakeJsonObject);
        Call<JsonObject> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.getDashboardData()).thenReturn(mockCall);

        // Execute test
        Response<JsonObject> response = mockApiService.getDashboardData().execute();

        // Verify results
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());

        // Verify last air quality reading
        JsonObject airQuality = response.body().getAsJsonObject("lastAirQuality");
        assertEquals(45, airQuality.get("aqi").getAsInt());
        assertEquals("Good", airQuality.get("category").getAsString());

        // Verify today's distance
        assertEquals(1250.5, response.body().get("todayDistance").getAsDouble(), 0.01);

        // Verify historical air quality readings
        JsonArray historicalData = response.body().getAsJsonArray("historicalAirQuality");
        assertEquals(2, historicalData.size());
        assertEquals("2024-12-10", historicalData.get(0).getAsJsonObject().get("date").getAsString());
        assertEquals("2024-12-11", historicalData.get(1).getAsJsonObject().get("date").getAsString());
    }

    /**
     * Tests dashboard data retrieval when no data is available
     * @throws Exception if an error occurs during API interaction
     *
     * Verifies the response when there's no dashboard data
     */
    @Test
    public void testGetDashboardData_noData() throws Exception {
        // Prepare test data for empty dashboard
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.addProperty("message", "No data available");
        fakeJsonObject.add("lastAirQuality", null);
        fakeJsonObject.addProperty("todayDistance", 0.0);
        fakeJsonObject.add("historicalAirQuality", new JsonArray());

        // Create mock response
        Response<JsonObject> fakeResponse = Response.success(fakeJsonObject);
        Call<JsonObject> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.getDashboardData()).thenReturn(mockCall);

        // Execute test
        Response<JsonObject> response = mockApiService.getDashboardData().execute();

        // Verify results
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("No data available", response.body().get("message").getAsString());
        assertTrue(response.body().get("historicalAirQuality").isJsonArray());
        assertEquals(0, response.body().getAsJsonArray("historicalAirQuality").size());
        assertEquals(0.0, response.body().get("todayDistance").getAsDouble(), 0.01);
    }
}
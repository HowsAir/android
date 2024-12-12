package com.example.mborper.breathbetter;

import com.example.mborper.breathbetter.api.ApiService;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;

import retrofit2.Call;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for current air quality map retrieval functionality
 * Utilizes the ApiService interface to mock API calls and verify current air quality map behavior.
 *
 * @author Alejandro Rosado
 * @since 2024-12-12
 */
public class CurrentAirQualityMapTest {

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
     * Tests successful retrieval of current air quality map
     * @throws Exception if an error occurs during API interaction
     *
     * Call<JsonObject> -> getCurrentAirQualityMap() -> Response<JsonObject>
     *
     * Verifies that the current air quality map response is successful and contains expected data.
     */
    @Test
    public void testGetCurrentAirQualityMap_success() throws Exception {
        // Prepare test data
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.addProperty("url", "https://example.com/air-quality-map-2024-12-12.png");
        fakeJsonObject.addProperty("timestamp", "2024-12-12T14:30:00Z");

        // Create mock response
        Response<JsonObject> fakeResponse = Response.success(fakeJsonObject);
        Call<JsonObject> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.getCurrentAirQualityMap()).thenReturn(mockCall);

        // Execute test
        Response<JsonObject> response = mockApiService.getCurrentAirQualityMap().execute();

        // Verify results
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());

        // Verify URL
        String url = response.body().get("url").getAsString();
        assertNotNull(url);
        assertTrue(url.startsWith("https://"));
        assertTrue(url.endsWith(".png"));

        // Verify timestamp
        String timestamp = response.body().get("timestamp").getAsString();
        assertNotNull(timestamp);
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    /**
     * Tests air quality map retrieval when no map is available
     * @throws Exception if an error occurs during API interaction
     *
     * Verifies the response when there's no current air quality map
     */
    @Test
    public void testGetCurrentAirQualityMap_noMapAvailable() throws Exception {
        // Prepare test data for no map
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.addProperty("message", "No current air quality map available");
        fakeJsonObject.add("url", null);
        fakeJsonObject.add("timestamp", null);

        // Create mock response
        Response<JsonObject> fakeResponse = Response.success(fakeJsonObject);
        Call<JsonObject> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.getCurrentAirQualityMap()).thenReturn(mockCall);

        // Execute test
        Response<JsonObject> response = mockApiService.getCurrentAirQualityMap().execute();

        // Verify results
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("No current air quality map available",
                response.body().get("message").getAsString());
        assertTrue(response.body().get("url").isJsonNull());
        assertTrue(response.body().get("timestamp").isJsonNull());
    }
}
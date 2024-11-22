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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for today's total distance retrieval functionality
 * Utilizes the ApiService interface to mock API calls and verify get total distance behavior.
 *
 * @author Alejandro Rosado
 * @since 2024-11-21
 */
public class TotalDistanceTest {

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
     * Tests successful retrieval of today's total distance
     * @throws Exception if an error occurs during API interaction
     *
     * Call<JsonObject> -> getTodayTotalDistance() -> Response<JsonObject>
     *
     * Verifies that the total distance response is successful and contains expected data.
     */
    @Test
    public void testGetTodayTotalDistance_success() throws Exception {
        // Prepare test data
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.addProperty("message", "Total distance retrieved successfully");
        fakeJsonObject.addProperty("totalDistance", 1500.5); // Distance in meters

        // Create mock response
        Response<JsonObject> fakeResponse = Response.success(fakeJsonObject);
        Call<JsonObject> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.getTodayTotalDistance()).thenReturn(mockCall);

        // Execute test
        Response<JsonObject> response = mockApiService.getTodayTotalDistance().execute();

        // Verify results
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("Total distance retrieved successfully",
                response.body().get("message").getAsString());
        assertEquals(1500.5,
                response.body().get("totalDistance").getAsDouble(),
                0.01); // Using delta for double comparison
    }

    /**
     * Tests case when no distance data is available for today
     * @throws Exception if an error occurs during API interaction
     *
     * Verifies that the response is successful but returns zero distance
     */
    @Test
    public void testGetTodayTotalDistance_noData() throws Exception {
        // Prepare test data for empty day
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.addProperty("message", "No distance recorded today");
        fakeJsonObject.addProperty("totalDistance", 0.0);

        // Create mock response
        Response<JsonObject> fakeResponse = Response.success(fakeJsonObject);
        Call<JsonObject> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.getTodayTotalDistance()).thenReturn(mockCall);

        // Execute test
        Response<JsonObject> response = mockApiService.getTodayTotalDistance().execute();

        // Verify results
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("No distance recorded today",
                response.body().get("message").getAsString());
        assertEquals(0.0,
                response.body().get("totalDistance").getAsDouble(),
                0.01);
    }
}
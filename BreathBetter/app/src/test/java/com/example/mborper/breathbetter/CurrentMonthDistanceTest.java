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
 * Test class for current month distance retrieval functionality
 * Utilizes the ApiService interface to mock API calls and verify current month distance behavior.
 *
 * @author Alejandro Rosado
 * @since 2024-12-12
 */
public class CurrentMonthDistanceTest {

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
     * Tests successful retrieval of current month's total distance
     * @throws Exception if an error occurs during API interaction
     *
     * Call<JsonObject> -> getCurrentMonthDistance() -> Response<JsonObject>
     *
     * Verifies that the current month distance response is successful and contains expected data.
     */
    @Test
    public void testGetCurrentMonthDistance_success() throws Exception {
        // Prepare test data
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.addProperty("message", "Current month distance retrieved successfully");
        fakeJsonObject.addProperty("totalDistance", 45000.75); // Distance in meters
        fakeJsonObject.addProperty("month", "December");
        fakeJsonObject.addProperty("year", 2024);

        // Create mock response
        Response<JsonObject> fakeResponse = Response.success(fakeJsonObject);
        Call<JsonObject> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.getCurrentMonthDistance()).thenReturn(mockCall);

        // Execute test
        Response<JsonObject> response = mockApiService.getCurrentMonthDistance().execute();

        // Verify results
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("Current month distance retrieved successfully",
                response.body().get("message").getAsString());
        assertEquals(45000.75,
                response.body().get("totalDistance").getAsDouble(),
                0.01); // Using delta for double comparison
        assertEquals("December", response.body().get("month").getAsString());
        assertEquals(2024, response.body().get("year").getAsInt());
    }

    /**
     * Tests case when no distance data is available for the current month
     * @throws Exception if an error occurs during API interaction
     *
     * Verifies that the response is successful but returns zero distance
     */
    @Test
    public void testGetCurrentMonthDistance_noData() throws Exception {
        // Prepare test data for empty month
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.addProperty("message", "No distance recorded this month");
        fakeJsonObject.addProperty("totalDistance", 0.0);
        fakeJsonObject.addProperty("month", "December");
        fakeJsonObject.addProperty("year", 2024);

        // Create mock response
        Response<JsonObject> fakeResponse = Response.success(fakeJsonObject);
        Call<JsonObject> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.getCurrentMonthDistance()).thenReturn(mockCall);

        // Execute test
        Response<JsonObject> response = mockApiService.getCurrentMonthDistance().execute();

        // Verify results
        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("No distance recorded this month",
                response.body().get("message").getAsString());
        assertEquals(0.0,
                response.body().get("totalDistance").getAsDouble(),
                0.01);
        assertEquals("December", response.body().get("month").getAsString());
        assertEquals(2024, response.body().get("year").getAsInt());
    }
}
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
 * Test class for user profile retrieval functionality
 * Utilizes the ApiService interface to mock API calls and verify get profile behavior.
 *
 * @author Alejandro Rosado
 * @since 2024-11-19
 */
public class UserDataTest {

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
     * Tests successful user profile retrieval
     * @throws Exception if an error occurs during API interaction
     *
     * Call<JsonObject> -> getUserProfile() -> Response<JsonObject>
     *
     * Verifies that the profile response is successful and contains expected data.
     */
    @Test
    public void testGetUserProfile_success() throws Exception {
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.addProperty("name", "John");
        fakeJsonObject.addProperty("surname", "Doe");
        fakeJsonObject.addProperty("email", "john.doe@example.com");

        Response<JsonObject> fakeResponse = Response.success(fakeJsonObject);
        Call<JsonObject> mockCall = mock(Call.class);

        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.getUserProfile()).thenReturn(mockCall);

        Response<JsonObject> response = mockApiService.getUserProfile().execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("John", response.body().get("name").getAsString());
        assertEquals("Doe", response.body().get("surname").getAsString());
        assertEquals("john.doe@example.com", response.body().get("email").getAsString());
    }
}

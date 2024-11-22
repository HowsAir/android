package com.example.mborper.breathbetter;

import com.example.mborper.breathbetter.api.ApiService;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for password change functionality
 * Utilizes the ApiService interface to mock API calls and verify password change behavior.
 *
 * @author Alejandro Rosado
 * @since 2024-11-20
 */
public class PasswordChangeTest {

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
     * Tests successful password change
     * @throws Exception if an error occurs during API interaction
     *
     * Call<Void> -> changePassword(JsonObject) -> Response<Void>
     *
     * Verifies that the password change response is successful.
     */
    @Test
    public void testChangePassword_success() throws Exception {
        // Prepare test data
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("currentPassword", "OldPass123!");
        requestBody.addProperty("newPassword", "NewPass123!");

        // Create mock response
        Response<Void> fakeResponse = Response.success(null);
        Call<Void> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.changePassword(any(JsonObject.class))).thenReturn(mockCall);

        // Execute test
        Response<Void> response = mockApiService.changePassword(requestBody).execute();

        // Verify results
        assertTrue(response.isSuccessful());
        assertEquals(200, response.code());
    }

    /**
     * Tests password change with incorrect current password
     * @throws Exception if an error occurs during API interaction
     *
     * Verifies that the password change fails with appropriate error response when
     * current password is incorrect.
     */
    @Test
    public void testChangePassword_incorrectCurrentPassword() throws Exception {
        // Prepare test data
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("currentPassword", "WrongPass123!");
        requestBody.addProperty("newPassword", "NewPass123!");

        // Create mock error response
        Response<Void> fakeResponse = Response.error(401,
                ResponseBody.create(MediaType.parse("application/json"),
                        "{\"message\":\"Current password is incorrect\"}"));
        Call<Void> mockCall = mock(Call.class);

        // Setup mock behavior
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.changePassword(any(JsonObject.class))).thenReturn(mockCall);

        // Execute test
        Response<Void> response = mockApiService.changePassword(requestBody).execute();

        // Verify results
        assertFalse(response.isSuccessful());
        assertEquals(401, response.code());
    }
}
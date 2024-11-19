package com.example.mborper.breathbetter;

import com.example.mborper.breathbetter.api.ApiService;

import org.junit.Before;
import org.junit.Test;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for user profile update functionality
 * Utilizes the ApiService interface to mock API calls and verify update behavior.
 *
 * @author Alejandro Rosado
 * @since 2024-11-19
 */
public class UserDataUpdateTest {

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
     * Tests successful user profile update
     * @throws Exception if an error occurs during API interaction
     *
     * Call<Void> -> updateUserProfile() -> Response<Void>
     *
     * Verifies that the update response is successful with expected HTTP status 200.
     */
    @Test
    public void testUpdateUserProfile_success() throws Exception {
        RequestBody fakeName = RequestBody.create(null, "John");
        RequestBody fakeSurnames = RequestBody.create(null, "Doe");
        MultipartBody.Part fakePhoto = MultipartBody.Part.createFormData("photo", "photo.jpg",
                RequestBody.create(null, "photoData".getBytes()));

        // Simulate a successful response with HTTP status 200 (default for Response.success)
        Response<Void> fakeResponse = Response.success(null);
        Call<Void> mockCall = mock(Call.class);

        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.updateUserProfile(fakeName, fakeSurnames, fakePhoto)).thenReturn(mockCall);

        Response<Void> response = mockApiService.updateUserProfile(fakeName, fakeSurnames, fakePhoto).execute();

        // Assert the response code matches the expected success code
        assertTrue(response.isSuccessful());
        assertEquals(200, response.code());
    }

    /**
     * Tests failed user profile update due to invalid data
     * @throws Exception if an error occurs during API interaction
     *
     * Call<Void> -> updateUserProfile() -> Response<Void>
     *
     * Verifies that the update fails and returns the appropriate HTTP status.
     */
    @Test
    public void testUpdateUserProfile_failure() throws Exception {
        RequestBody fakeName = RequestBody.create(null, "");  // Invalid data
        Response<Void> fakeErrorResponse = Response.error(400, okhttp3.ResponseBody.create(null, "Bad Request"));
        Call<Void> mockCall = mock(Call.class);

        when(mockCall.execute()).thenReturn(fakeErrorResponse);
        when(mockApiService.updateUserProfile(fakeName, null, null)).thenReturn(mockCall);

        Response<Void> response = mockApiService.updateUserProfile(fakeName, null, null).execute();

        // Assert the response code matches the expected failure code
        assertEquals(400, response.code());
    }
}
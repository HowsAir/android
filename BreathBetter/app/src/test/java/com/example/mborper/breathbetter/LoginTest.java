package com.example.mborper.breathbetter;

import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.login.pojos.LoginRequest;
import com.example.mborper.breathbetter.login.pojos.LoginResponse;
import com.example.mborper.breathbetter.api.models.User;

import org.junit.Before;
import org.junit.Test;

import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for user login functionality
 * Utilizes the ApiService interface to mock API calls and verify login behavior.
 * <p>
 * @since 2024-11-04
 * last edited: 2024-11-08
 * @author Alejandro Rosado
 */
public class LoginTest {

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
     * Tests a successful login attempt
     * @throws Exception if an error occurs during API interaction
     * <p>
     * LoginRequest -> login() -> Response<LoginResponse>
     * <p>
     * Verifies that the login response is successful and that user details and
     * an authentication token are correctly retrieved.
     */
    @Test
    public void testLogin_success() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        User fakeUser = new User();
        fakeUser.setName("Test User");
        fakeUser.setSurname("Smith");
        fakeUser.setEmail("test@example.com");
        fakeUser.setPassword("password123");

        LoginResponse fakeLoginResponse = new LoginResponse();
        fakeLoginResponse.setUser(fakeUser);
        fakeLoginResponse.setMessage("Login successful");

        Headers fakeHeaders = Headers.of("Set-Cookie", "auth_token=fakeAuthToken; Path=/; HttpOnly");

        Response<LoginResponse> fakeResponse = Response.success(fakeLoginResponse, fakeHeaders);
        Call<LoginResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.login(loginRequest)).thenReturn(mockCall);

        Response<LoginResponse> response = mockApiService.login(loginRequest).execute();

        List<String> cookies = response.headers().values("Set-Cookie");
        String authToken = cookies.stream()
                .filter(cookie -> cookie.startsWith("auth_token="))
                .map(cookie -> cookie.split(";")[0].substring("auth_token=".length()))
                .findFirst()
                .orElse(null);

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals("Login successful", response.body().getMessage());
        assertNotNull(response.body().getUser());
        assertEquals("Test User", response.body().getUser().getName());
        assertEquals("Smith", response.body().getUser().getSurname());
        assertEquals("test@example.com", response.body().getUser().getEmail());
        assertEquals("fakeAuthToken", authToken);
    }

    /**
     * Tests a failed login attempt
     * @throws Exception if an error occurs during API interaction
     * <p>
     * LoginRequest -> login() -> Response<LoginResponse>
     * <p>
     * Verifies that a login attempt with invalid credentials returns an HTTP 401 error.
     */
    @Test
    public void testLogin_failure() throws Exception {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");

        Response<LoginResponse> fakeErrorResponse = Response.error(401, ResponseBody.create(null, "Unauthorized"));
        Call<LoginResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(fakeErrorResponse);
        when(mockApiService.login(loginRequest)).thenReturn(mockCall);

        Response<LoginResponse> response = mockApiService.login(loginRequest).execute();

        assertFalse(response.isSuccessful());
        assertEquals(401, response.code());
    }
}

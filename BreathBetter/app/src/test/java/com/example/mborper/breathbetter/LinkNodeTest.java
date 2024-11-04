package com.example.mborper.breathbetter;

import com.example.mborper.breathbetter.api.ApiService;

import org.junit.Before;
import org.junit.Test;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @brief Test class for linking a node to a user
 *
 * @author Alejandro Rosado
 *
 * Tests linking functionality in ApiService to ensure nodes can be correctly associated with users.
 */
public class LinkNodeTest {

    private ApiService mockApiService;

    /**
     * @brief Sets up mock API service before each test
     *
     * Creates a mock instance of ApiService to simulate API interactions for testing.
     */
    @Before
    public void setUp() {
        mockApiService = mock(ApiService.class);
    }

    /**
     * @brief Tests a successful attempt to link a node to a user
     *
     * @throws Exception if an error occurs during API interaction
     *
     * String -> linkNodeToUser() -> Response<Void>
     *
     * Verifies that the linkNodeToUser method returns a successful HTTP 200 response.
     */
    @Test
    public void testLinkNodeToUser_success() throws Exception {
        String nodeId = "12345";

        Response<Void> fakeResponse = Response.success(null);
        Call<Void> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(fakeResponse);
        when(mockApiService.linkNodeToUser(nodeId)).thenReturn(mockCall);

        Response<Void> response = mockApiService.linkNodeToUser(nodeId).execute();

        assertTrue(response.isSuccessful());
        assertEquals(200, response.code());
    }

    /**
     * @brief Tests a failed attempt to link a node to a user
     *
     * @throws Exception if an error occurs during API interaction
     *
     * String -> linkNodeToUser() -> Response<Void>
     *
     * Verifies that the linkNodeToUser method returns a failed HTTP 400 response
     * when an invalid request is made.
     */
    @Test
    public void testLinkNodeToUser_failure() throws Exception {
        String nodeId = "12345";

        Response<Void> fakeErrorResponse = Response.error(400, ResponseBody.create(null, "Bad Request"));
        Call<Void> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(fakeErrorResponse);
        when(mockApiService.linkNodeToUser(nodeId)).thenReturn(mockCall);

        Response<Void> response = mockApiService.linkNodeToUser(nodeId).execute();

        assertFalse(response.isSuccessful());
        assertEquals(400, response.code());
    }
}

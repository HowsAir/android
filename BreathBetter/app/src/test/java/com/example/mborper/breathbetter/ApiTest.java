package com.example.mborper.breathbetter;

import org.junit.Before;
import org.junit.Test;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.measurements.Measurement;


/**
 * Unit tests for the ApiService class.
 * <p>
 * This class tests the behavior of ApiService when sending data, specifically
 * checking success and failure responses for the sendMeasurement method.
 * <p>
 * @author Manuel Borregales
 * @since 2024-10-08
 * last edited: 2024-11-08
 */
public class ApiTest {

    private ApiService mockApiService;

    /**
     * Sets up a mock instance of ApiService before each test.
     */
    @Before
    public void setUp() {
        // Creation of a mock of ApiService
        mockApiService = mock(ApiService.class);
    }

    /**
     * Tests the sendMeasurement method in a success scenario.
     * <p>
     * Simulates a successful response when sending measurement data and verifies the
     * correctness of the response data for O3 value, latitude, and longitude.
     * <p>
     * @throws Exception if there is an error during execution
     */
    @Test
    public void testSendData_success() throws Exception {
        Measurement fakeMeasurement = new Measurement();

        fakeMeasurement.setO3Value(50);
        fakeMeasurement.setLatitude(39.4699);
        fakeMeasurement.setLongitude(-0.3763);

        // Successful fake response creation
        Response<Measurement> fakeResponse = Response.success(fakeMeasurement);

        Call<Measurement> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(fakeResponse);

        when(mockApiService.sendMeasurement(fakeMeasurement)).thenReturn(mockCall);

        Response<Measurement> response = mockApiService.sendMeasurement(fakeMeasurement).execute();

        assertTrue(response.isSuccessful());
        assertEquals(50, (int) response.body().getO3Value());
        assertEquals(39.4699, response.body().getLatitude(), 0.0001);
        assertEquals(-0.3763, response.body().getLongitude(), 0.0001);
    }

    /**
     * Tests the sendMeasurement method in a failure scenario.
     * <p>
     * Simulates an unsuccessful response when sending measurement data and verifies
     * the response code matches the expected error code.
     * <p>
     * @throws Exception if there is an error during execution
     */
    @Test
    public void testSendMeasurement_failure() throws Exception {
        Measurement fakeMeasurement = new Measurement();

        fakeMeasurement.setO3Value(50);
        fakeMeasurement.setLatitude(39.4699);
        fakeMeasurement.setLongitude(-0.3763);

        // Unsuccesful fake response creation
        Response<Measurement> fakeErrorResponse = Response.error(400, ResponseBody.create(null, "Bad Request"));

        Call<Measurement> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(fakeErrorResponse);

        when(mockApiService.sendMeasurement(fakeMeasurement)).thenReturn(mockCall);

        Response<Measurement> response = mockApiService.sendMeasurement(fakeMeasurement).execute();

        assertFalse(response.isSuccessful());
        assertEquals(400, response.code());
    }
}


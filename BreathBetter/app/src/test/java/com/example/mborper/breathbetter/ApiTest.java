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

public class ApiTest {

    private ApiService mockApiService;

    @Before
    public void setUp() {
        // Creation of a mock of ApiService
        mockApiService = mock(ApiService.class);
    }

    @Test
    public void testSendData_success() throws Exception {
        Measurement fakeMeasurement = new Measurement();

        fakeMeasurement.setPpm(50);
        fakeMeasurement.setTemperature(25);
        fakeMeasurement.setLatitude(39.4699);
        fakeMeasurement.setLongitude(-0.3763);

        // Successful fake response creation
        Response<Measurement> fakeResponse = Response.success(fakeMeasurement);

        Call<Measurement> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(fakeResponse);

        when(mockApiService.sendMeasurement(fakeMeasurement)).thenReturn(mockCall);

        Response<Measurement> response = mockApiService.sendMeasurement(fakeMeasurement).execute();

        assertTrue(response.isSuccessful());
        assertEquals(50, (int) response.body().getPpm());  // Conversión a int si es necesario
        assertEquals(25, (int) response.body().getTemperature());
        assertEquals(39.4699, response.body().getLatitude(), 0.0001);
        assertEquals(-0.3763, response.body().getLongitude(), 0.0001);
    }

    @Test
    public void testSendMeasurement_failure() throws Exception {
        Measurement fakeMeasurement = new Measurement();

        fakeMeasurement.setPpm(50);
        fakeMeasurement.setTemperature(25);
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


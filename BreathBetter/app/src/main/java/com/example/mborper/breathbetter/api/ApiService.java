package com.example.mborper.breathbetter.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * The ApiService interface defines the API endpoints that the client will interact with.
 * It uses Retrofit's annotations to define GET and POST requests for sending and receiving measurements.
 *
 * @author  Manuel Borregales
 * @date:    04/10/2024
 */
public interface ApiService {

    /**
     * Fetches the list of measurements from the API using a GET request.
     *
     * @return Call object encapsulating a List of Measurement objects.
     */
    @GET("measurements/")
    Call<List<Measurement>> getMeasurements();

    /**
     * Sends a new measurement to the API using a POST request.
     *
     * @param measurement The Measurement object to be sent to the server.
     * @return Call object encapsulating the response, including the posted Measurement object.
     */
    @POST("measurements/")
    Call<Measurement> sendMeasurement(@Body Measurement measurement);
}


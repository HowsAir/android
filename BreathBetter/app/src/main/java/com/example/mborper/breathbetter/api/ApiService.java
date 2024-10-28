package com.example.mborper.breathbetter.api;

import com.example.mborper.breathbetter.login.pojo.LoginRequest;
import com.example.mborper.breathbetter.login.pojo.LoginResponse;
import com.example.mborper.breathbetter.measurements.Measurement;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Author: Manuel Borregales
 * Date: 04/10/2024
 *
 * The ApiService interface defines the API endpoints that the client will interact with.
 * It uses Retrofit's annotations to define GET and POST requests for sending and receiving measurements.
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

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    /**
     * Links a node to the current user
     *
     * @param nodeId The ID of the node to link
     * @return Call object encapsulating the response
     */
    @PUT("nodes/{nodeId}/link")
    Call<Void> linkNodeToUser(@Path("nodeId") String nodeId);
}


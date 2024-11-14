package com.example.mborper.breathbetter.api;

import com.example.mborper.breathbetter.api.models.Node;
import com.example.mborper.breathbetter.login.pojos.LoginRequest;
import com.example.mborper.breathbetter.login.pojos.LoginResponse;
import com.example.mborper.breathbetter.measurements.Measurement;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * The ApiService interface defines the API endpoints that the client will interact with.
 * It uses Retrofit's annotations to define GET and POST requests for sending and receiving measurements.
 *
 * @author Manuel Borregales
 * @since 2024-10-04
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

    /**
     * Authenticates a user by sending login credentials to the API.
     * <p>
     * This method sends a LoginRequest object with user credentials to the server,
     * returning a Call object that encapsulates the LoginResponse, which contains authentication data.
     *
     * @param loginRequest the LoginRequest object containing user login credentials
     * @return Call object encapsulating the response with authentication details in a LoginResponse
     */
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

    /**
     * Sends a request to the API to generate a password reset code and send it to the user's email.
     *
     * @param requestBody A JSON object containing the email of the user requesting the password reset.
     * @return Call object encapsulating the response.
     */
    @POST("auth/forgot-password-code")
    Call<Void> forgotPasswordCode(@Body JsonObject requestBody);

    /**
     * Verifies the password reset code provided by the user.
     *
     * @param email The email of the user requesting the password reset.
     * @param code The password reset code provided by the user.
     * @return Call object encapsulating the response.
     */
    @GET("auth/forgot-password-token")
    Call<Void> forgotPasswordToken(@Query("email") String email, @Query("code") String code);

    /**
     * Resets the user's password to the new password provided.
     *
     * @param requestBody A JSON object containing the new password.
     * @return Call object encapsulating the response.
     */
    @PUT("users/reset-password")
    Call<Void> resetPassword(@Body JsonObject requestBody);

    /**
     * Retrieves the node associated with the authenticated user.
     *
     * Sends a GET request to the `/users/node` endpoint and returns the node details
     * associated with the current user, if available.
     *
     * @return Call object encapsulating the response, containing the Node object if the request is successful.
     *         - 200: Node object with node details.
     *         - 404: If no node is found for the user.
     */
    @GET("users/node")
    Call<JsonObject> getUserNode();
}


package com.example.mborper.breathbetter.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @GET("measurements/")
    Call<List<Measurement>> getMeasurements();  // Si esperas una lista de mediciones

    @POST("measurements/")
    Call<Measurement> sendMeasurement(@Body Measurement measurement);  // Envías una medición al servidor
}

package com.example.mborper.breathbetter.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Author: Manuel Borregales
 * Date: 04/10/2024
 *
 * The ApiClient class configures and provides a Retrofit instance to communicate with the API backend.
 * This class sets up the base URL for the API, logging for HTTP requests, and includes a converter factory
 * to handle JSON responses using Gson.
 */
public class ApiClient {

    // Static Retrofit instance, used to interact with the API.
    private static Retrofit retrofit = null;

    /**
     * Returns a Retrofit client instance. If the client is null, a new instance is created and configured
     * with a base URL, a JSON converter, and an HTTP logging interceptor.
     *
     * @return Retrofit instance for making API requests.
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Logs HTTP request/response bodies

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor) // Add logging interceptor
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.32.132:3000/api/v1/") // Base URL of the API
                    .addConverterFactory(GsonConverterFactory.create()) // Use Gson to handle JSON
                    .client(client) // Use OkHttpClient for handling requests
                    .build();
        }
        return retrofit;
    }
}


package com.example.mborper.breathbetter.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
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
                    .addInterceptor(interceptor) // Your existing interceptor
                    .cookieJar(new CookieJar() {  // Add the cookie jar here
                        private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

                        @Override
                        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                            cookieStore.put(url.host(), cookies);
                        }

                        @Override
                        public List<Cookie> loadForRequest(HttpUrl url) {
                            List<Cookie> cookies = cookieStore.get(url.host());
                            return cookies != null ? cookies : new ArrayList<>();
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl("http://192.168.1.150:3000/api/v1/") // Base URL of the API
                    .addConverterFactory(GsonConverterFactory.create()) // Use Gson to handle JSON
                    .client(client) // Use OkHttpClient for handling requests
                    .build();
        }
        return retrofit;
    }
}


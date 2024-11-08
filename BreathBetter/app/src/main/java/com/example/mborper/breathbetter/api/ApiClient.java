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
import android.content.Context;

import com.example.mborper.breathbetter.login.SessionManager;
/**
 * The ApiClient class configures and provides a Retrofit instance to communicate with the API backend.
 * This class sets up the base URL for the API, logging for HTTP requests, and includes a converter factory
 * to handle JSON responses using Gson.
 *
 * @author Manuel Borregales
 * @since 2024-10-04
 */
public class ApiClient {

    private static Retrofit retrofit = null;
    /**
     * Returns a Retrofit client instance. If the client is null, a new instance is created and configured
     * with a base URL, a JSON converter, and an HTTP logging interceptor.
     *
     * @return Retrofit instance for making API requests.
     */
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = new SessionManager(context); // Initialize the session manager

            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .addInterceptor(new AuthInterceptor(sessionManager)) // Add the custom interceptor here
                    .cookieJar(new CookieJar() {
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
                    .baseUrl("http://192.168.32.237:3000/api/v1/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}


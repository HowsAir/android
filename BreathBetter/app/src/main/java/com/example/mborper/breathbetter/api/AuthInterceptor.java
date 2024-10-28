package com.example.mborper.breathbetter.api;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import com.example.mborper.breathbetter.login.SessionManager;

public class AuthInterceptor implements Interceptor {

    private SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Avoid adding the token to the login request
        if (originalRequest.url().encodedPath().equals("/auth/login")) {
            return chain.proceed(originalRequest);
        }

        // Get the token from sessionManager
        String authToken = sessionManager.getAuthToken();

        // If the token is available, add it to the headers
        if (authToken != null) {
            Request.Builder requestBuilder = originalRequest.newBuilder()
                    .header("Authorization", "Bearer " + authToken); // Add your auth token in the Authorization header
            Request request = requestBuilder.build();
            return chain.proceed(request);
        }

        // If no token is available, proceed with the original request
        return chain.proceed(originalRequest);
    }
}


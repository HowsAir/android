package com.example.mborper.breathbetter.api;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import com.example.mborper.breathbetter.login.SessionManager;

/**
 * Interceptor that adds an authorization token to HTTP requests.
 * <p>
 * This interceptor automatically includes an auth token in the request headers
 * (as a cookie) for all endpoints except the login endpoint.
 *
 * @author Juan Díaz
 * @since 2024-10-28
 */
public class AuthInterceptor implements Interceptor {

    private SessionManager sessionManager;

    /**
     * Constructs an AuthInterceptor with a SessionManager to manage token retrieval.
     *
     * @param sessionManager the session manager instance to obtain the auth token
     */
    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * Intercepts HTTP requests to add an authorization token if available.
     * <p>
     * Skips adding the token for login requests (`/auth/login`). If a token is available
     * from the SessionManager, it is added as a "Cookie" header in the request.
     *
     * @param chain the interceptor chain for proceeding with the request
     * @return the response after the request has been intercepted and modified if necessary
     * @throws IOException if an error occurs during request execution
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Avoid adding the token to the login request
        if (originalRequest.url().encodedPath().equals("/auth/login")) {
            return chain.proceed(originalRequest);
        }

        // Get the token from sessionManager
        String authToken = sessionManager.getAuthToken();

        // If the token is available, add it to the cookies
        if (authToken != null) {
            Request modifiedRequest = originalRequest.newBuilder()
                    .header("Cookie", "auth_token=" + authToken) // Add the auth token as a cookie
                    .build();
            return chain.proceed(modifiedRequest);
        }

        // Proceed with the original request if no token is available
        return chain.proceed(originalRequest);
    }
}

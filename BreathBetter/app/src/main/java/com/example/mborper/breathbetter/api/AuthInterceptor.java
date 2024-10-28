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

        // Evita agregar el token a la solicitud de inicio de sesión
        if (originalRequest.url().encodedPath().equals("/auth/login")) {
            return chain.proceed(originalRequest);
        }

        // Obtén el token del sessionManager
        String authToken = sessionManager.getAuthToken();

        // Si el token está disponible, agrégalo a las cookies
        if (authToken != null) {
            Request modifiedRequest = originalRequest.newBuilder()
                    .header("Cookie", "auth_token=" + authToken) // Agrega el auth token como cookie
                    .build();
            return chain.proceed(modifiedRequest);
        }

        // Si no hay un token disponible, procede con la solicitud original
        return chain.proceed(originalRequest);
    }
}

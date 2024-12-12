package com.example.mborper.breathbetter.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Activity responsible for displaying the map screen and handling bottom navigation interactions.
 * This activity provides navigation to different parts of the app using a BottomNavigationView.
 *
 * @author Alejandro Rosado
 * @since  2024-12-11
 * last updated 2024-12-12
 */
public class MapsActivity extends AppCompatActivity {

    private WebView webView;
    private ExecutorService executorService;

    private static final String TAG = "MapsActivity";

    /**
     * Called when the activity is created.
     * <p>
     * Initializes the activity by setting the layout and setting up the bottom navigation menu.
     * @param savedInstanceState The saved state of the activity, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.map);
        setupBottomNavigation();

        // Configurar WebView
        webView = findViewById(R.id.webview_mapa);

        WebView.setWebContentsDebuggingEnabled(true);

        // Configuraciones de WebView para interactividad
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        // Configurar WebViewClient para manejar la carga de recursos
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("MapsActivity", "Page finished loading: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.e("WebViewError", "Error loading resource: " + error.getDescription());
            }

        });

        // Cargar el HTML directamente
        loadHtmlContent();
    }

    /**
     * Loads HTML content into the WebView by fetching a URL from the API.
     * <p>
     * This method retrieves the current air quality map URL from the server and downloads the HTML content
     * to be displayed in the WebView.
     */
    private void loadHtmlContent() {
        // Create a service for API calls
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);

        // Make the API call to get the map URL
        Call<JsonObject> call = apiService.getCurrentAirQualityMap();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String mapUrl = response.body().get("url").getAsString();
                    String timestamp = response.body().get("timestamp").getAsString();

                    // Download HTML content from the received URL
                    ExecutorService executorService = Executors.newSingleThreadExecutor();
                    executorService.execute(() -> {
                        try {
                            URL url = new URL(mapUrl);
                            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                            StringBuilder content = new StringBuilder();
                            String inputLine;

                            while ((inputLine = in.readLine()) != null) {
                                content.append(inputLine).append("\n");
                            }
                            in.close();

                            String htmlContent = content.toString();

                            // Load the downloaded content in the WebView
                            runOnUiThread(() -> {
                                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
                                // Optional: Log the timestamp of the map
                                Log.d(TAG, "Map loaded from: " + mapUrl + " at " + timestamp);
                            });

                        } catch (IOException e) {
                            Log.e(TAG, "Error downloading HTML: " + e.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "Failed to retrieve map URL: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching map URL: " + t.getMessage());
            }
        });
    }


    /**
     * Configures the bottom navigation menu and defines the behavior when each item is selected.
     * <p>
     * Each navigation item (home, map, target, profile) will start a new activity or perform a transition
     * when selected. If the current item is the same as the one already selected, no action is performed.
     */
    private void setupBottomNavigation() {
    BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(item ->

    {
        if (item.getItemId() == R.id.home) {
            startActivity(new Intent(this, MainActivity.class));
            return true;
        } else if (item.getItemId() == R.id.map) {
            overridePendingTransition(0, 0);
            return true;
        } else if (item.getItemId() == R.id.target) {
            startActivity(new Intent(this, GoalActivity.class));
            overridePendingTransition(0, 0);
            return true;
        } else if (item.getItemId() == R.id.profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            overridePendingTransition(0, 0);
            return true;
        }
        return false;
    });
}

}

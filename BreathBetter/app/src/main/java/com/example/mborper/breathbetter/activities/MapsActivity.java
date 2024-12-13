package com.example.mborper.breathbetter.activities;

import static com.example.mborper.breathbetter.activities.BaseActivity.setCurrentScreen;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public class MapsActivity extends BaseActivity {

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

        //BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        //bottomNavigationView.setSelectedItemId(R.id.map);
        setCurrentScreen("MAP");
        setupBottomNavigation();


        Button showPopupButton = findViewById(R.id.show_popup_button); // Este botón debe estar definido en el layout XML
        showPopupButton.setOnClickListener(v -> showPopupDialog());

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupBottomNavigation();
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

    private void showPopupDialog() {
        // Inflate the popup layout (which contains ViewPager2 and paginator)
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.gas_info_popup, null);

        // Aplicar bordes redondeados directamente al diseño del popup
        popupView.setBackgroundResource(R.drawable.rounded_dialog_background);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(popupView);
        AlertDialog dialog = builder.create();

        // Setup the ViewPager2 and paginator dots
        ViewPager2 viewPager = popupView.findViewById(R.id.viewpager_content);
        viewPager.post(() -> {
            // Asegúrate de que el ViewPager2 se ajuste correctamente
            viewPager.requestLayout();
        });

        LinearLayout dotsLayout = popupView.findViewById(R.id.dots_paginator);

        // Create example page content (texts, icons, and titles)
        List<PageContent> pages = new ArrayList<>();

        // Page 1
        pages.add(new PageContent(
                "Ozono (O3)",  // Title for Page 1
                Arrays.asList(
                        "Origen: vehículos, fábricas tras reaccionar con la luz del sol. ",
                        "Efectos: Dificultad para respirar, agrava el asma. ",
                        "Tip: Evita actividades al aire libre de 10:00 a 17:00."
                ),
                Arrays.asList(R.drawable.ic_origin, R.drawable.ic_effects, R.drawable.ic_tip)
        ));

        // Page 2
        pages.add(new PageContent(
                "Monóxido de Carbono (CO2)",  // Title for Page 2
                Arrays.asList(
                        "Origen: Calefacción y combustión de gasolina y diesel. ",
                        "Efectos: Mareos, fatiga; peligro en alta exposición",
                        "Tip: Evita motores encendidos en espacios cerrados."
                ),
                Arrays.asList(R.drawable.ic_origin, R.drawable.ic_effects, R.drawable.ic_tip)
        ));

        // Page 3
        pages.add(new PageContent(
                "Dióxido de nitrógeno (NO2)",  // Title for Page 3
                Arrays.asList(
                        "Origen: Tráfico y combustibles fósiles. ",
                        "Efectos: Irrita las vías respiratorias, reduce función pulmonar. ",
                        "Tip: Mantén ventanas cerradas en horas pico."
                ),
                Arrays.asList(R.drawable.ic_origin, R.drawable.ic_effects, R.drawable.ic_tip)
        ));

        // Page 4
        pages.add(new PageContent(
                "Tips generales",  // Title for Page 4
                Arrays.asList(
                        "Ventila tu casa temprano o tarde, y usa purificadores",
                        "Opta por rutas con menos tráfico",
                        "Usa mascarillas en días de alta contaminación."
                ),
                Arrays.asList(R.drawable.circle, R.drawable.circle, R.drawable.circle)
        ));

        // Set up the adapter for ViewPager2 using the pageContent list
        PopupPagerAdapter adapter = new PopupPagerAdapter(pages);
        viewPager.setAdapter(adapter);

        // Set up the paginator dots
        setupDots(pages.size(), dotsLayout, viewPager);

        // Set up the close button
        Button closeButton = popupView.findViewById(R.id.close_popup_button);
        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Show the dialog
        dialog.show();

        if (dialog.getWindow() != null) {
            // Fondo transparente para el Window
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Ajustar tamaño del diálogo manualmente
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.copyFrom(dialog.getWindow().getAttributes());
        // Convertir 320dp a píxeles
        int heightInPixels = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                420,
                getResources().getDisplayMetrics()
        );

        // Usar el valor convertido para la altura del diálogo
        params.width = heightInPixels; // Ajusta según tus necesidades
        params.height = WindowManager.LayoutParams.WRAP_CONTENT; // Ajusta según tus necesidades
        dialog.getWindow().setAttributes(params);

        // Agregar márgenes al popup
        int marginHorizontal = getResources().getDimensionPixelSize(R.dimen.popup_margin_horizontal); // Define en dimens.xml
        dialog.getWindow().getDecorView().setPadding(marginHorizontal, 0, marginHorizontal, 0);
    }


    private void setupDots(int numPages, LinearLayout dotsLayout, ViewPager2 viewPager) {
        dotsLayout.removeAllViews();
        ImageView[] dots = new ImageView[numPages];

        for (int i = 0; i < numPages; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageResource(R.drawable.dot_inactive);  // Asegúrate de tener este recurso en tus drawable
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dots[i].setLayoutParams(params);
            dotsLayout.addView(dots[i]);
        }

        // Cambiar el estado activo al deslizar
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < numPages; i++) {
                    dots[i].setImageResource(i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
                }
            }
        });

        // Configurar el estado inicial
        dots[0].setImageResource(R.drawable.dot_active);  // Asegúrate de tener este recurso en tus drawable
    }

}

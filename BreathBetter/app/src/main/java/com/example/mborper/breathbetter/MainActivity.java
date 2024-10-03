package com.example.mborper.breathbetter;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {

    private static final String log_tag = "DEVELOPMENT_LOG";
    private Intent serviceIntent = null;
    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Obtener una instancia de ApiService
        apiService = ApiClient.getClient().create(ApiService.class);

        getMeasurements();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void onStartServiceButtonClicked(View v) {
        Log.d(log_tag, "Start service button clicked");

        if (this.serviceIntent != null) {
            // The service is already running
            return;
        }

        Log.d(log_tag, "MainActivity: Starting the service");

        this.serviceIntent = new Intent(this, BeaconListeningService.class);

        this.serviceIntent.putExtra("waitTime", 5000L);

        // Use startForegroundService() instead of startService() for Android 8.0+
        startForegroundService(this.serviceIntent);

    } // onStartServiceButtonClicked

    public void onStopServiceButtonClicked(View v) {

        if (this.serviceIntent == null) {
            // The service is not running
            return;
        }

        stopService(this.serviceIntent);

        this.serviceIntent = null;
        Log.d(log_tag, "Stop service button clicked");

    } // onStopServiceButtonClicked

    public void onMakeRequestButtonClicked(View v) {
        // Hacer una llamada POST para enviar una medición
        Measurement newMeasurement = new Measurement();  // Crea y configura un nuevo objeto Measurement
        newMeasurement.setPpm(35.0);
        newMeasurement.setTemperature(25.0);
        newMeasurement.setLatitude(39.4699);
        newMeasurement.setLongitude(-0.3763);

        Call<Measurement> postCall = apiService.sendMeasurement(newMeasurement);
        postCall.enqueue(new Callback<Measurement>() {
            @Override
            public void onResponse(Call<Measurement> call, Response<Measurement> response) {
                if (response.isSuccessful()) {
                    Measurement measurement = response.body();
                    Log.d("MainActivity", "Measurement sent successfully: ");
                }
            }

            @Override
            public void onFailure(Call<Measurement> call, Throwable t) {
                Log.e("MainActivity", "Error sending measurement: " + t.getMessage());
            }
        });
    }

    private void getMeasurements() {
        // Hacer una llamada GET para obtener las mediciones
        Call<List<Measurement>> call = apiService.getMeasurements();
        call.enqueue(new Callback<List<Measurement>>() {
            @Override
            public void onResponse(Call<List<Measurement>> call, Response<List<Measurement>> response) {
                if (response.isSuccessful()) {
                    List<Measurement> measurements = response.body();
                    // Aquí puedes manejar la lista de mediciones
                    Log.d("MainActivity", "Received measurements: " + measurements.size());
                }
            }

            @Override
            public void onFailure(Call<List<Measurement>> call, Throwable t) {
                // Aquí manejas errores
                Log.e("MainActivity", "Error: " + t.getMessage());
            }
        });
    }

}
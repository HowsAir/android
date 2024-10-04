package com.example.mborper.breathbetter;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.example.mborper.breathbetter.bluetooth.BeaconListeningService;
import android.Manifest;

import android.content.pm.PackageManager;

import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.api.Measurement;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import static android.content.Context.RECEIVER_NOT_EXPORTED;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "DEVELOPMENT_LOG";
    private static final int PERMISSION_REQUEST_CODE = 8888;
    private Intent serviceIntent = null;
    private ApiService apiService;
    private BeaconListeningService beaconListeningService;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private static final String TARGET_UUID = "EQUIPO-JAVIER-3A";
    private Measurement lastReceivedMeasurement;
    private BroadcastReceiver measurementReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        requestPermissions();
        isBluetoothEnabled();
        registerBroadcaster();
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    private void registerBroadcaster() {
        measurementReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.mborper.breathbetter.MEASUREMENT_RECEIVED".equals(intent.getAction())) {
                    lastReceivedMeasurement = (Measurement) intent.getSerializableExtra("measurement");
                    Log.d(LOG_TAG, "Received measurement: " + lastReceivedMeasurement);
                }
            }
        };

        IntentFilter filter = new IntentFilter("com.example.mborper.breathbetter.MEASUREMENT_RECEIVED");

        // Use registerReceiver with the RECEIVER_NOT_EXPORTED flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(measurementReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(measurementReceiver, filter);
        }
    }

    private void isBluetoothEnabled() {
        // Inicializa el lanzador para habilitar Bluetooth
        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(LOG_TAG, "Bluetooth enabled by user");
                        // Inicia el servicio cuando el Bluetooth está habilitado
                        Intent serviceIntent = new Intent(this, BeaconListeningService.class);
                        startService(serviceIntent);
                    } else {
                        Log.e(LOG_TAG, "Bluetooth enabling was canceled by user");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver to prevent memory leaks
        if (measurementReceiver != null) {
            unregisterReceiver(measurementReceiver);
        }
    }

    public void onStartServiceButtonClicked(View v) {
        Log.d(LOG_TAG, "Start service button clicked");

        if (this.serviceIntent != null) {
            return;
        }

        // Revisa si el Bluetooth ya está habilitado
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (bta != null && !bta.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            // Inicia el servicio directamente si Bluetooth está habilitado
            Intent serviceIntent = new Intent(this, BeaconListeningService.class);
            startService(serviceIntent);
        }

        Log.d(LOG_TAG, "MainActivity: Starting the service");

        this.serviceIntent = new Intent(this, BeaconListeningService.class);
        this.serviceIntent.putExtra("targetDeviceUUID", TARGET_UUID); // Replace with your beacon's name

        startForegroundService(this.serviceIntent);
        beaconListeningService = new BeaconListeningService();
    }

    public void onStopServiceButtonClicked(View v) {
        if (this.serviceIntent == null) {
            return;
        }

        stopService(this.serviceIntent);
        this.serviceIntent = null;
        beaconListeningService = null;
        Log.d(LOG_TAG, "Stop service button clicked");
    }

    public void onMakeRequestButtonClicked(View v) {
        if (lastReceivedMeasurement != null) {
            sendMeasurementToApi(lastReceivedMeasurement);
        } else {
            Log.e(LOG_TAG, "No measurement available to send");
            // Optionally, show a message to the user
        }
    }


    private void sendMeasurementToApi(Measurement measurement) {

    /*    if(measurement != null) {
            measurement.setPpm(2);
            measurement.setTemperature(3);
            measurement.setLongitude(4);
            measurement.setLatitude(5);
        }
*/
        Call<Measurement> postCall = apiService.sendMeasurement(measurement);
        postCall.enqueue(new Callback<Measurement>() {
            @Override
            public void onResponse(Call<Measurement> call, Response<Measurement> response) {
                if (response.isSuccessful()) {
                    Log.d(LOG_TAG, "Measurement sent successfully");
                } else {
                    Log.e(LOG_TAG, "Error sending measurement to the API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Measurement> call, Throwable t) {
                Log.e(LOG_TAG, "Error sending measurement: " + t.getMessage());
            }
        });
    }

    private void requestPermissions() {
        // Verifica si los permisos necesarios no han sido concedidos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Solicita los permisos necesarios
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.FOREGROUND_SERVICE,
                            Manifest.permission.FOREGROUND_SERVICE_LOCATION // Nuevo permiso añadido
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "onRequestPermissionResult(): permissions granted!");
            } else {
                Log.d(LOG_TAG, "onRequestPermissionResult(): Help: permissions NOT granted!");
            }
        }
    }
}
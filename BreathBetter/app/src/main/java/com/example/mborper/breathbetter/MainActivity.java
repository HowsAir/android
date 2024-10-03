package com.example.mborper.breathbetter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanSettings;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;

import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.api.Measurement;
import com.example.mborper.breathbetter.bluetooth.IBeaconFrame;
import com.example.mborper.breathbetter.bluetooth.Utilities;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String log_tag = "DEVELOPMENT_LOG";
    private Intent serviceIntent = null;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private ApiService apiService;
    private static final int PERMISSION_REQUEST_CODE = 8888;

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

    /**
     * Displays information about a detected BTLE device.
     *
     * @param result The result from the scan, containing the BTLE device information.
     */
    private void displayBTLEDeviceInformation(ScanResult result) {
        BluetoothDevice bluetoothDevice = result.getDevice();
        byte[] bytes = result.getScanRecord().getBytes();
        int rssi = result.getRssi();

        Log.d(log_tag, " ****************************************************");
        Log.d(log_tag, " ****** BTLE DEVICE DETECTED *********************** ");
        Log.d(log_tag, " ****************************************************");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Request necessary permissions if they are missing
            return;
        }

        Log.d(log_tag, " name = " + bluetoothDevice.getName());
        Log.d(log_tag, " toString = " + bluetoothDevice.toString());
        Log.d(log_tag, " address = " + bluetoothDevice.getAddress());
        Log.d(log_tag, " rssi = " + rssi);
        Log.d(log_tag, " bytes = " + new String(bytes));
        Log.d(log_tag, " bytes (" + bytes.length + ") = " + Utilities.bytesToHexString(bytes));

        IBeaconFrame tib = new IBeaconFrame(bytes);

        Log.d(log_tag, " ----------------------------------------------------");
        Log.d(log_tag, " prefix = " + Utilities.bytesToHexString(tib.getPrefix()));
        Log.d(log_tag, " advFlags = " + Utilities.bytesToHexString(tib.getAdvFlags()));
        Log.d(log_tag, " advHeader = " + Utilities.bytesToHexString(tib.getAdvHeader()));
        Log.d(log_tag, " companyID = " + Utilities.bytesToHexString(tib.getCompanyID()));
        Log.d(log_tag, " iBeacon type = " + Integer.toHexString(tib.getIBeaconType()));
        Log.d(log_tag, " iBeacon length = " + Integer.toHexString(tib.getIBeaconLength()) + " (" + tib.getIBeaconLength() + ")");
        Log.d(log_tag, " uuid = " + Utilities.bytesToHexString(tib.getUUID()));
        Log.d(log_tag, " uuid = " + Utilities.bytesToString(tib.getUUID()));
        Log.d(log_tag, " major = " + Utilities.bytesToHexString(tib.getMajor()) + " (" + Utilities.bytesToInt(tib.getMajor()) + ")");
        Log.d(log_tag, " minor = " + Utilities.bytesToHexString(tib.getMinor()) + " (" + Utilities.bytesToInt(tib.getMinor()) + ")");
        Log.d(log_tag, " txPower = " + Integer.toHexString(tib.getTxPower()) + " (" + tib.getTxPower() + ")");
        Log.d(log_tag, " ****************************************************");
    }

    /**
     * Starts scanning for a specific BTLE device based on its name. A scan callback is installed for the device search.
     *
     * @param targetDevice The name of the device to search for.
     */
    private void searchSpecificBTLEDevice(final String targetDevice) {
        this.scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d(log_tag, "searchSpecificBTLEDevice(): onScanResult()");

                displayBTLEDeviceInformation(result);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                Log.d(log_tag, "searchSpecificBTLEDevice(): onBatchScanResults()");
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.d(log_tag, "searchSpecificBTLEDevice(): onScanFailed()");
            }
        };

        ScanFilter filter = new ScanFilter.Builder().setDeviceName(targetDevice).build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        Log.d(log_tag, "searchSpecificBTLEDevice(): starting scan for: " + targetDevice);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        this.scanner.startScan(filters, new ScanSettings.Builder().build(), this.scanCallback);
    }

    /**
     * Stops scanning for BTLE devices. If no scan is currently in progress, nothing happens.
     */
    private void stopBTLEDeviceSearch() {
        if (this.scanCallback == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        this.scanner.stopScan(this.scanCallback);
        this.scanCallback = null;
    }

    /**
     * Initializes the Bluetooth adapter and requests necessary permissions.
     * This method retrieves the default Bluetooth adapter, enables it,
     * and initializes the Bluetooth Low Energy (BLE) scanner. It checks for
     * required permissions and requests them if not granted.
     */
    private void initializeBluetooth() {
        Log.d(log_tag, "initializeBluetooth(): obtaining Bluetooth adapter");

        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();

        Log.d(log_tag, "initializeBluetooth(): enabling Bluetooth adapter");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            // public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission.
            // See the documentation for ActivityCompat#requestPermissions for more details.
            return;
        }
        bta.enable();

        Log.d(log_tag, "initializeBluetooth(): enabled = " + bta.isEnabled());

        Log.d(log_tag, "initializeBluetooth(): state = " + bta.getState());

        Log.d(log_tag, "initializeBluetooth(): obtaining BLE scanner");

        this.scanner = bta.getBluetoothLeScanner();

        if (this.scanner == null) {
            Log.d(log_tag, "initializeBluetooth(): Help: NO BLE scanner obtained!!!!");
        }

        Log.d(log_tag, "initializeBluetooth(): requesting permissions (if not already granted)!!!!");

        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            Log.d(log_tag, "initializeBluetooth(): it seems I ALREADY have the necessary permissions!!!!");
        }
    }


    /**
     * Callback method invoked when the user responds to a permission request.
     * This method handles the result of the permission request initiated in the
     * {@link #initializeBluetooth()} method. It checks whether the permissions
     * were granted or denied and logs the outcome.
     *
     * @param requestCode The request code passed in the request.
     * @param permissions The requested permissions.
     * @param grantResults The results of the permission requests.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                // If the request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d(log_tag, "onRequestPermissionResult(): permissions granted!!!!");
                    // Permission is granted. Continue the action or workflow in your app.
                } else {
                    Log.d(log_tag, "onRequestPermissionResult(): Help: permissions NOT granted!!!!");
                }
                return;
        }
        // Other 'case' lines can be added to check for other
        // permissions this app might request.
    }


}
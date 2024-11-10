package com.example.mborper.breathbetter.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.mborper.breathbetter.R;
import com.example.mborper.breathbetter.api.ApiClient;
import com.example.mborper.breathbetter.api.ApiService;
import com.example.mborper.breathbetter.login.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * It's the activity that asks the user for the permissions needed for qr scanning
 *
 * @author Juan Diaz & Manuel Borregales
 * @since  2024-10-26
 */
public class QRExplanationActivity extends AppCompatActivity {

    private final int QR_REQUEST_CODE = 24;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private final int MANUAL_INPUT_REQUEST_CODE = 25;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_explanation);

        apiService = ApiClient.getClient(this).create(ApiService.class);
        sessionManager = new SessionManager(this);

        // Click listener for the manual input button
        findViewById(R.id.buttonNocamera).setOnClickListener(v -> startManualInput());

    }

    /**
     * Initiates the QR scanner by first checking for camera permissions. If the permissions
     * are not granted, it requests them from the user. Once permissions are confirmed,
     * it starts the QRScannerActivity.
     */
    public void startScanner(View v) {
        // Check if camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request camera permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startQRScannerActivity();
        }
    }


    /**
     * After the permission is granted, it starts the activity
     */
    private void startQRScannerActivity() {
        Intent intent = new Intent(this, QRScannerActivity.class);
        startActivityForResult(intent, QR_REQUEST_CODE);
    }

    /**
     * Called when the user responds to a permission request. This method receives the result
     * of the camera permission request initiated in startScanner(). If the permission is granted,
     * it starts the QRScannerActivity. If denied, it shows a message explaining that the permission is required.
     *
     * @param requestCode The integer request code originally passed to requestPermissions(), allowing you to
     *                    identify which permission request this result corresponds to.
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions, which is either
     *                     PackageManager.PERMISSION_GRANTED or PackageManager.PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("qr", "Permission granted in result.");
                // Start QR scanner activity after permission is granted
                startQRScannerActivity();
            } else {
                Log.d("qr", "Permission denied.");
                // Show a message indicating that camera permission is required
                Toast.makeText(this, "Camera permission is required to scan QR codes.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Starts the ManualInputActivity for manual node ID entry. This method is called when
     * the user chooses to enter the node ID manually instead of scanning a QR code.
     *
     * The method creates an intent to launch ManualInputActivity and starts it
     * with startActivityForResult to receive the entered node ID when the activity finishes.
     */
    private void startManualInput() {
        Intent intent = new Intent(this, ManualInputActivity.class);
        startActivityForResult(intent, MANUAL_INPUT_REQUEST_CODE);
    }


    /**
     * Called when an activity that was launched exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(),
     *                    allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data An Intent, which can return result data to the caller (various data can be attached to it).
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String nodeId = null;

            if (requestCode == QR_REQUEST_CODE) {
                nodeId = data.getStringExtra(QRScannerActivity.QR_RESULT);
            } else if (requestCode == MANUAL_INPUT_REQUEST_CODE) {
                nodeId = data.getStringExtra(ManualInputActivity.MANUAL_RESULT);
            }

            if (nodeId != null) {
                linkNodeToUser(this, nodeId);
                sessionManager.saveNodeId(nodeId);
                Toast.makeText(this, "ID del nodo: " + nodeId, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Sends the nodeId to the API via HTTP put request.
     *
     *                   nodeId: String   ---> linkNodeToUser()
     *
     * @param nodeId String
     */
    private void linkNodeToUser(Context context, String nodeId) {
        apiService.linkNodeToUser(nodeId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(context, "Nodo vinculado exitosamente", Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(QRExplanationActivity.this, MainActivity.class));
                        } else {
                            Toast.makeText(context, "Error al vincular el nodo", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

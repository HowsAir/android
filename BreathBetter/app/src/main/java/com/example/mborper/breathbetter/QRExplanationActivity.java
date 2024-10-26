package com.example.mborper.breathbetter;

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

/**
 * It's the activity that asks the user for the permissions needed for qr scanning
 *
 * @author Juan Diaz & Manuel Borregales
 * date:  2024-10-26
 */
public class QRExplanationActivity extends AppCompatActivity {

    private final int QR_REQUEST_CODE = 24;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_explanation);
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
        if (requestCode == QR_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String qrResult = data.getStringExtra(QRScannerActivity.QR_RESULT);

            //Here we would call the API to linkNodeToUser
            Toast.makeText(this, "QR escaneado: " + qrResult, Toast.LENGTH_LONG).show();
        }
    }
}

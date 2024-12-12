package com.example.mborper.breathbetter.measurements;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.text.SimpleDateFormat;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.example.mborper.breathbetter.activities.MainActivity;
import com.example.mborper.breathbetter.R;

import android.os.Handler;
import android.util.Log;

import java.util.Date;
import java.util.Locale;


/**
 * GasAlertManager
 * <p>
 * Class responsible for monitoring gas levels and issuing alerts when dangerous
 * levels are detected. It plays an alarm sound and shows a notification with
 * details about the gas level, timestamp, and location when the gas concentration
 * exceeds a predefined threshold.
 * <p>
 * It uses Android's Notification system to create an alert channel for gas warnings,
 * and MediaPlayer to play alert sounds. It interacts with the LocationUtils to
 * retrieve the user's current location for the notification.
 *
 * @author Manuel Borregales
 * @author Alejandro Rosado
 * @since 2024-10-23
 * last updated 2024-12-12
 */

public class GasAlertManager {
    private static final String LOG_TAG = "GasAlertManager";
    private static final String ALERT_CHANNEL_ID = "GAS_ALERT_CHANNEL";
    private static final String ERROR_CHANNEL_ID = "SENSOR_ERROR_CHANNEL";
    private static final int ALERT_NOTIFICATION_ID = 2;
    private static final int ERROR_NOTIFICATION_ID = 3;

    // Gas concentration threshold in PPM (Parts Per Million) to trigger an alert
    private static final int PPM_DANGER_THRESHOLD = 100; // Adjust based on official guidelines

    // Gas concentration threshold in PPM (Parts Per Million) to trigger an error
    private static final int PPM_MAX_VALID_VALUE = 1000;

    // New constants for different notifications error scenarios
    private static final String ERROR_NO_NODE = "NODO_NO_ENCONTRADO";
    private static final String ERROR_CONNECTION_LOST = "CONEXION_PERDIDA";


    // Existing timeout for beacon scan
    private static final long BEACON_TIMEOUT_MS = 40000; // 40 seconds
    private static final long MEASUREMENT_LOSS_TIMEOUT_MS = 40000;

    // Threshold for consecutive missed measurements
    private static final int MAX_CONSECUTIVE_MISSED_MEASUREMENTS = 3;

    private long lastSuccessfulMeasurementTimestamp = 0;

    // Context, NotificationManager, LocationUtils and MediaPlayer instances
    private final Context context;
    private final NotificationManager notificationManager;
    private final LocationUtils locationUtils;
    private MediaPlayer alertSound;
    private long lastBeaconTimestamp;
    private final Handler timeoutHandler;
    public boolean isErrorNotified;
    // To check if inactivity feature is enabled
    private boolean isRunning = false;

    private boolean isFirstRun = true;
    private NodeConnectionState connectionState;

    /**
     * Constructor for GasAlertManager
     * <p>
     * Initializes the notification manager, location utilities, and the alert
     * sound. Sets up a notification channel for gas alerts.
     *
     * @param context Application context needed to initialize services.
     */
    public GasAlertManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.locationUtils = new LocationUtils(context);
        initializeAlertChannel(); // Set up the notification channel
        initializeErrorChannel(); // Set up the error notification channel
        initializeAlertSound(); // Prepare the alert sound
        isErrorNotified = false;
        isRunning = true;
        this.connectionState = NodeConnectionState.getInstance();

        lastBeaconTimestamp = System.currentTimeMillis();
        timeoutHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Creates the notification channel for gas alerts.
     * <p>
     * This channel is required for Android 8.0 and above to issue notifications.
     * The channel enables vibration and sets a default sound to notify the user
     * of dangerous gas levels.
     */
    private void initializeAlertChannel() {
        NotificationChannel alertChannel = new NotificationChannel(
                ALERT_CHANNEL_ID,
                "Gas Alert Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        alertChannel.setDescription("Alerts for dangerous gas levels");
        alertChannel.enableVibration(true);
        alertChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
        alertChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttributes);

        notificationManager.createNotificationChannel(alertChannel);
    }

    /**
     * Creates the error notification channel for error alerts.
     * <p>
     * This channel is required for Android 8.0 and above to issue notifications.
     * The channel enables vibration and sets a default sound to notify the user
     * of errors in the sensor.
     */
    private void initializeErrorChannel() {
        NotificationChannel errorChannel = new NotificationChannel(
                ERROR_CHANNEL_ID,
                "Sensor Error Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        errorChannel.setDescription("Alertas de errores del sensor");
        errorChannel.enableVibration(true);
        errorChannel.setVibrationPattern(new long[]{0, 500, 500, 500});

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build();
        errorChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), audioAttributes);

        notificationManager.createNotificationChannel(errorChannel);
    }

    /**
     * Initializes the sound to be played when a gas alert is triggered.
     * <p>
     * Uses the default alarm sound from the device's ringtone manager and sets
     * it to play without looping.
     */
    private void initializeAlertSound() {
        Uri alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        alertSound = MediaPlayer.create(context, alertSoundUri);
        alertSound.setLooping(false);
    }

    /**
     * Checks the gas level and triggers an alert if it exceeds the danger threshold.
     * <p>
     * If the detected gas level in PPM is greater than the threshold, the method
     * sends an alert with the gas concentration, the current timestamp, and the
     * user's location.
     *
     * @param o3Value The detected gas concentration in PPM.
     */
    public void checkAndAlert(int o3Value) {
        // Update timestamp from the last measurement
        lastBeaconTimestamp = System.currentTimeMillis();

        // Verify errors from the sensor
        if (o3Value < 0 || o3Value > PPM_MAX_VALID_VALUE) {
            sendSensorErrorNotification("LECTURA_ERRONEA",
                    "El sensor está reportando valores erroneos");
            return;
        }

        // If there are not errors, check if the gas level is dangerous
        if (o3Value > PPM_DANGER_THRESHOLD) {
            String timestamp = TimeUtils.getCurrentTimestamp();
            //String location = locationUtils.getLocationString(locationUtils.getCurrentLocation());
            sendAlert(o3Value, timestamp);
        }
    }

    /**
     * Sends a notification and plays an alert sound when a dangerous gas level is detected.
     * <p>
     * The notification contains details such as the gas concentration in PPM, the
     * timestamp of detection, and the user's location. It also plays an alert sound.
     *
     * @param o3Value The gas concentration in PPM.
     * @param timestamp The timestamp when the gas level was detected.
     */
    private void sendAlert(int o3Value, String timestamp) {

        // Parse and format the timestamp
        String formattedTimestamp = formatTimestamp(timestamp);

        // Play alert sound
        if (alertSound != null && !alertSound.isPlaying()) {
            alertSound.start();
        }

        // Create an intent to open MainActivity when the notification is clicked
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.howsair_logo)
                .setContentTitle("¡Alerta de Gas!")
                .setContentText("Nivel de gas peligroso detectado: " + o3Value + " PPM")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Nivel de gas peligroso detectado: " + o3Value + " PPM\n" +
                                "Hora: " + formattedTimestamp))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Issue the notification
        notificationManager.notify(ALERT_NOTIFICATION_ID, builder.build());
    }

    /**
     * Formats the timestamp into a human-readable format.
     * <p>
     * Converts the timestamp from the ISO 8601 format into a user-friendly format
     * for display in notifications.
     *
     * @param timestamp The original timestamp to format.
     * @return The formatted timestamp.
     */
    private String formatTimestamp(String timestamp) {
        try {
            // Assuming the timestamp is in ISO 8601 format: "yyyy-MM-dd'T'HH:mm:ss"
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);

            // Output format: "HH:mm dd/MM/yyyy"
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return timestamp; // Return original if parsing fails
        }
    }

    /**
     * Sends a notification to inform the user of a sensor error.
     * <p>
     * This method builds and displays a notification with details about the
     * error type, description, timestamp, and location. The notification opens
     * the main activity when tapped, and auto-cancels when the user interacts with it.
     *
     * @param errorType    a brief identifier for the type of error (e.g., "SIN_SEÑAL")
     * @param errorDetails additional details explaining the error
     */
    private void sendSensorErrorNotification(String errorType, String errorDetails) {
        String timestamp = TimeUtils.getCurrentTimestamp();
        String formattedTimestamp = formatTimestamp(timestamp);

        // Intent para abrir MainActivity al hacer clic en la notificación
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        // Construcción de la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
                .setSmallIcon(R.drawable.howsair_logo)
                .setContentTitle("Error en tu Nodo Sensor")
                .setContentText(errorType + ": " + errorDetails)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Tipo de error: " + errorType + "\n" +
                                "Detalles: " + errorDetails + "\n" +
                                "Hora: " + formattedTimestamp))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Emitir la notificación
        notificationManager.notify(ERROR_NOTIFICATION_ID, builder.build());
    }

    /**
     * Resets the 'isFirstRun' flag, allowing the system to reinitialize.
     * This can be used to trigger reinitialization if necessary after the first run.
     */
    public void resetFirstRunState() {
        isFirstRun = true;
    }

    /**
     * Updates the timestamp of the last received beacon.
     * This is useful for tracking the time when the last beacon was received.
     */
    public void updateLastBeaconTimestamp() {
        lastBeaconTimestamp = System.currentTimeMillis();
    }

    /**
     * Handles the measurement received from the sensor.
     * <p>
     * This method validates the received measurement and processes it if valid.
     * If the measurement is valid, it checks and triggers an alert if necessary.
     * Otherwise, it sends a sensor error notification.
     *
     * @param o3Value The received gas concentration measurement in PPM.
     */
    public void onMeasurementReceived(int o3Value) {
        boolean isValidMeasurement = isValidMeasurement(o3Value);
        connectionState.updateConnectionState(isValidMeasurement);

        if (isValidMeasurement) {
            checkAndAlert(o3Value);
        } else {
            sendSensorErrorNotification("MEDICION_INVALIDA", "Medición fuera de rango");
        }
    }

    /**
     * Validates if the received measurement is within the acceptable range.
     * <p>
     * A valid measurement is a non-negative value and must be less than or equal to the maximum allowable PPM value.
     *
     * @param o3Value The gas concentration value in PPM to validate.
     * @return true if the measurement is valid, false otherwise.
     */
    private boolean isValidMeasurement(int o3Value) {
        return o3Value >= 0 && o3Value <= PPM_MAX_VALID_VALUE;
    }

    /**
     * Handles reconnection logic if the connection state is in the "reconnecting" state.
     * <p>
     * If the connection state indicates that the system is in the process of reconnecting, it sends a notification
     * about the reconnection attempt and can trigger further reconnection logic if needed.
     */
    private void handleReconnectionIfNeeded() {
        if (connectionState.getConnectionStatus() == NodeConnectionState.ConnectionStatus.RECONNECTING) {
            sendReconnectionNotification();
            // Lógica adicional de reconexión si es necesario
        }
    }

    /**
     * Checks if the beacon signal is active by comparing the current time with
     * the last beacon timestamp. If the difference is less than `BEACON_TIMEOUT_MS`,
     * the error notification flag is reset, indicating a healthy signal.
     */
    private void checkBeaconTime() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBeaconTimestamp < BEACON_TIMEOUT_MS) {
            isErrorNotified = false;
        }
    }

    /**
     * Releases resources used by the alert sound player.
     * <p>
     * This method is called to clean up resources, such as the MediaPlayer,
     * when the GasAlertManager is no longer needed.
     */
    public void cleanup() {
        isRunning = false;

        if (alertSound != null) {
            alertSound.release();
            alertSound = null;
        }

        // Clean notifications
        notificationManager.cancel(ALERT_NOTIFICATION_ID);
        notificationManager.cancel(ERROR_NOTIFICATION_ID);
    }

    /**
     * Sends a notification to inform the user that the node has reconnected and is sending measurements again.
     * <p>
     * This method uses Android's NotificationManager to create and show a high-priority notification that alerts the user
     * when the system has re-established the connection and is receiving measurements.
     */
    private void sendReconnectionNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
                .setSmallIcon(R.drawable.howsair_logo)
                .setContentTitle("Conexión restaurada")
                .setContentText("El nodo ha vuelto a enviar medidas.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(ERROR_NOTIFICATION_ID, builder.build());
    }
}

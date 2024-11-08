package com.example.mborper.breathbetter.measurements;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.example.mborper.breathbetter.MainActivity;
import com.example.mborper.breathbetter.R;

import android.os.Handler;


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
 * @since 2024-10-23
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

    // Timeout for the BLE scan to throw error
    private static final long BEACON_TIMEOUT_MS = 30000;

    // Context, NotificationManager, LocationUtils and MediaPlayer instances
    private final Context context;
    private final NotificationManager notificationManager;
    private final LocationUtils locationUtils;
    private MediaPlayer alertSound;
    private long lastBeaconTimestamp;
    private final Handler timeoutHandler;
    private final Runnable timeoutChecker;
    public boolean isErrorNotified;

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

        lastBeaconTimestamp = System.currentTimeMillis();
        timeoutHandler = new Handler(Looper.getMainLooper());
        timeoutChecker = new Runnable() {
            @Override
            public void run() {
                if (!isErrorNotified) {
                    checkBeaconTimeout();
                }
                checkBeaconTime();
                timeoutHandler.postDelayed(this, 5000); // Verify every 5 seconds
            }
        };
        startTimeoutChecking();
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
            String location = locationUtils.getLocationString(locationUtils.getCurrentLocation());
            sendAlert(o3Value, timestamp, location);
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
     * @param location The user's current location.
     */
    private void sendAlert(int o3Value, String timestamp, String location) {
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
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("¡Alerta de Gas!")
                .setContentText("Nivel de gas peligroso detectado: " + o3Value + " PPM")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Nivel de gas peligroso detectado: " + o3Value + " PPM\n" +
                                "Hora: " + timestamp + "\n" +
                                "Ubicación: " + location))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Issue the notification
        notificationManager.notify(ALERT_NOTIFICATION_ID, builder.build());
    }

    /**
     * Sends a notification and plays an alert sound when the sensor is not working properly.
     * <p>
     * The notification contains details such as the failure to read gas or the inactivity of the sensor
     * //@param //ppm The gas concentration in PPM.
     * //@param //timestamp The timestamp when the gas level was detected.
     * //@param //location The user's current location.
     */
    private void sendSensorErrorNotification(String errorType, String errorDetails) {
        String timestamp = TimeUtils.getCurrentTimestamp();
        String location = locationUtils.getLocationString(locationUtils.getCurrentLocation());

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Error en tu Nodo Sensor")
                .setContentText(errorType + ": " + errorDetails)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Error en tu Nodo Sensor\n" +
                                "Tipo de error: " + errorType + "\n" +
                                "Detalles: " + errorDetails + "\n" +
                                "Hora: " + timestamp + "\n" +
                                "Ubicación: " + location))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(ERROR_NOTIFICATION_ID, builder.build());
    }

    private void startTimeoutChecking() {
        timeoutHandler.post(timeoutChecker);
    }

    private void checkBeaconTimeout() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBeaconTimestamp > BEACON_TIMEOUT_MS) {
            sendSensorErrorNotification("SIN_SEÑAL",
                    "No se han recibido datos del sensor en los últimos 30 segundos, comprueba la conexion");
            isErrorNotified = true;
        }
    }

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
        if (alertSound != null) {
            alertSound.release();
            alertSound = null;
        }
    }
}

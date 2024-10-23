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
import androidx.core.app.NotificationCompat;

import com.example.mborper.breathbetter.MainActivity;
import com.example.mborper.breathbetter.R;

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
 * date: 2024-10-23
 */

public class GasAlertManager {
    // Logging tag for error or info messages
    private static final String LOG_TAG = "GasAlertManager";

    // Channel ID for the gas alert notification
    private static final String ALERT_CHANNEL_ID = "GAS_ALERT_CHANNEL";

    // ID for the gas alert notification
    private static final int ALERT_NOTIFICATION_ID = 2;

    // Gas concentration threshold in PPM (Parts Per Million) to trigger an alert
    private static final int PPM_DANGER_THRESHOLD = 100; // Adjust based on official guidelines

    // Context, NotificationManager, LocationUtils and MediaPlayer instances
    private final Context context;
    private final NotificationManager notificationManager;
    private final LocationUtils locationUtils;
    private MediaPlayer alertSound;

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
        initializeAlertSound(); // Prepare the alert sound
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
     * @param ppm The detected gas concentration in PPM.
     */
    public void checkAndAlert(int ppm) {
        if (ppm > PPM_DANGER_THRESHOLD) {
            String timestamp = TimeUtils.getCurrentTimestamp();
            String location = locationUtils.getLocationString(locationUtils.getCurrentLocation());
            sendAlert(ppm, timestamp, location); // Trigger alert notification and sound
        }
    }

    /**
     * Sends a notification and plays an alert sound when a dangerous gas level is detected.
     * <p>
     * The notification contains details such as the gas concentration in PPM, the
     * timestamp of detection, and the user's location. It also plays an alert sound.
     *
     * @param ppm The gas concentration in PPM.
     * @param timestamp The timestamp when the gas level was detected.
     * @param location The user's current location.
     */
    private void sendAlert(int ppm, String timestamp, String location) {
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
                .setContentText("Nivel de gas peligroso detectado: " + ppm + " PPM")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Nivel de gas peligroso detectado: " + ppm + " PPM\n" +
                                "Hora: " + timestamp + "\n" +
                                "Ubicación: " + location))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Issue the notification
        notificationManager.notify(ALERT_NOTIFICATION_ID, builder.build());
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

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
import android.util.Log;

import com.example.mborper.breathbetter.MainActivity;
import com.example.mborper.breathbetter.R;

public class GasAlertManager {
    private static final String LOG_TAG = "GasAlertManager";
    private static final String ALERT_CHANNEL_ID = "GAS_ALERT_CHANNEL";
    private static final int ALERT_NOTIFICATION_ID = 2;
    private static final int PPM_DANGER_THRESHOLD = 100; // Adjust according to official limits

    private final Context context;
    private final NotificationManager notificationManager;
    private final LocationUtils locationUtils;
    private MediaPlayer alertSound;

    public GasAlertManager(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.locationUtils = new LocationUtils(context);
        initializeAlertChannel();
        initializeAlertSound();
    }

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

    private void initializeAlertSound() {
        Uri alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        alertSound = MediaPlayer.create(context, alertSoundUri);
        alertSound.setLooping(false);
    }

    public void checkAndAlert(int ppm) {
        if (ppm > PPM_DANGER_THRESHOLD) {
            String timestamp = TimeUtils.getCurrentTimestamp();
            String location = locationUtils.getLocationString(locationUtils.getCurrentLocation());
            sendAlert(ppm, timestamp, location);
        }
    }

    private void sendAlert(int ppm, String timestamp, String location) {
        // Play alert sound
        if (alertSound != null && !alertSound.isPlaying()) {
            alertSound.start();
        }

        // Create notification
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

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

        notificationManager.notify(ALERT_NOTIFICATION_ID, builder.build());
    }

    public void cleanup() {
        if (alertSound != null) {
            alertSound.release();
            alertSound = null;
        }
    }
}

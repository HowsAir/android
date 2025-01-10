package com.example.mborper.breathbetter.measurements;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.mborper.breathbetter.activities.MainActivity;

public class NotificationUtils {

    public static PendingIntent getPendingIntentToOpenApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE // Usa FLAG_IMMUTABLE para seguridad en Android 12+
        );
    }
}

package com.example.mborper.breathbetter.measurements;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.example.mborper.breathbetter.activities.MainActivity;

/**
 * Utility class for handling notification-related tasks.
 *
 * @author Alejandro Rosado
 * @since 2025-01-10
 */
public class NotificationUtils {

    /**
     * Creates a PendingIntent to open the app's main activity.
     * <p>
     * Context -> getPendingIntentToOpenApp() -> PendingIntent
     *
     * @param context The application context used to create the intent.
     * @return A PendingIntent to open the main activity.
     */
    public static PendingIntent getPendingIntentToOpenApp(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE // Ensures security compliance for Android 12+
        );
    }
}

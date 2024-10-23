package com.example.mborper.breathbetter.measurements;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public static String getCurrentTimestamp() {
        return dateFormat.format(new Date());
    }

    public static String formatTimestamp(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }
}
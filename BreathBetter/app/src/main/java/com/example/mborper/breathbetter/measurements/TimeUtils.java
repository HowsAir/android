package com.example.mborper.breathbetter.measurements;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * TimeUtils
 * <p>
 * Utility class that provides methods to handle timestamps and format date-time
 * values. It can return the current timestamp in a specific format, or convert
 * a given timestamp (in milliseconds) to a formatted date-time string. The
 * default format is "yyyy-MM-dd HH:mm:ss".
 *
 * @author Manuel Borregales
 * date: 2024-10-23
 */

public class TimeUtils {
    // Date format pattern for representing date and time in "yyyy-MM-dd HH:mm:ss" format
    private static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    /**
     * Returns the current timestamp as a formatted string.
     * <p>
     * This method retrieves the current date and time, and formats it into
     * the pattern "yyyy-MM-dd HH:mm:ss".
     *
     * @return A string representing the current timestamp in the format "yyyy-MM-dd HH:mm:ss".
     */
    public static String getCurrentTimestamp() {
        return dateFormat.format(new Date());
    }

    /**
     * Converts a given timestamp (in milliseconds) to a formatted date-time string.
     * <p>
     * This method takes a timestamp (long value) and formats it into a human-readable
     * date and time using the same pattern: "yyyy-MM-dd HH:mm:ss".
     *
     * @param timestamp The timestamp in milliseconds since January 1, 1970.
     * @return A string representing the formatted date-time for the given timestamp.
     */
    public static String formatTimestamp(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }
}

package io.nxt3.ioclassic;


import android.content.Context;
import android.util.TypedValue;

import java.util.Calendar;

/**
 * Contains helper functions which are used in IOClassicWatchFaceService
 * (Moved hear for readability)
 */
class HelperFunctions {

    /**
     * Converts density pixels to pixels
     *
     * @param context context for getResources()
     * @param dp      desired density pixels
     * @return converted dp to pixels
     */
    static float dpToPx(Context context, final int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    /**
     * Converts scale pixels to pixels -- used for setting text sizes
     *
     * @param context context for getResources()
     * @param sp      desired scale pixels pixels
     * @return converted sp to pixels
     */
    static int spToPx(Context context, final float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                context.getResources().getDisplayMetrics());
    }

    /**
     * Gets the hour String based on the input hour
     * This is done so that it's possible to translate the hour numbers!
     *
     * @param context context for getString()
     * @param hour    to find the String for
     * @return String of the hour number
     */
    static String getHourLabel(Context context, int hour) {
        switch (hour) {
            case 1:
                return context.getString(R.string.hour_label_1);
            case 2:
                return context.getString(R.string.hour_label_2);
            case 3:
                return context.getString(R.string.hour_label_3);
            case 4:
                return context.getString(R.string.hour_label_4);
            case 5:
                return context.getString(R.string.hour_label_5);
            case 6:
                return context.getString(R.string.hour_label_6);
            case 7:
                return context.getString(R.string.hour_label_7);
            case 8:
                return context.getString(R.string.hour_label_8);
            case 9:
                return context.getString(R.string.hour_label_9);
            case 10:
                return context.getString(R.string.hour_label_10);
            case 11:
                return context.getString(R.string.hour_label_11);
            case 12:
                return context.getString(R.string.hour_label_12);
            default:
                return "";
        }
    }

    /**
     * Determines if the current time is between fromMillis and toMillis
     * Does so by calculating an inverse time range based on the minutes
     *
     * @param currentMillis current time in milliseconds
     * @param fromMillis    start time in milliseconds
     * @param toMillis      end time in milliseconds
     * @return whether or not the currentMillis is between fromMillis and toMillis
     */
    static boolean isTimeInRange(long currentMillis, long fromMillis, long toMillis) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentMillis);

        final int currentMinuteOfDay
                = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        calendar.setTimeInMillis(fromMillis);

        final int fromMinuteOfDay
                = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        calendar.setTimeInMillis(toMillis);

        final int toMinuteOfDay
                = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

        if (fromMinuteOfDay <= toMinuteOfDay) {
            return (currentMinuteOfDay >= fromMinuteOfDay && currentMinuteOfDay < toMinuteOfDay);
        } else {
            return (currentMinuteOfDay >= fromMinuteOfDay || currentMinuteOfDay < toMinuteOfDay);
        }
    }
}

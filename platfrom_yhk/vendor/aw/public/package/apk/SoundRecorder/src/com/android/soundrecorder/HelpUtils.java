package com.android.soundrecorder;

import java.util.concurrent.TimeUnit;

public class HelpUtils {
    public static String formatMilliseconds(long milliseconds) {
        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) - TimeUnit.HOURS.toMinutes(hours);
        long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) - TimeUnit.MINUTES.toSeconds(remainingMinutes) - TimeUnit.HOURS.toSeconds(hours);

        return String.format("%02d:%02d:%02d", hours, remainingMinutes, remainingSeconds);
    }
}

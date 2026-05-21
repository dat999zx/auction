package com.bidify.common.utility;

import java.time.Duration;
import java.time.LocalDateTime;

public class TimeUtil {
    public static Duration parseHHMM(String hhmm) {
        if (hhmm == null || !hhmm.contains(":")) return Duration.ZERO;
        try {
            String[] parts = hhmm.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return Duration.ofHours(hours).plusMinutes(minutes);
        } catch (Exception e) {
            return Duration.ZERO;
        }
    }

    public static String formatHHMM(Duration duration) {
        if (duration == null) return "00:00";
        long totalMinutes = duration.toMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}

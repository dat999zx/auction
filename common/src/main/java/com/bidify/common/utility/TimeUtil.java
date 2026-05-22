package com.bidify.common.utility;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private TimeUtil() {}

    public static LocalDateTime nowInVietnam() {
        return LocalDateTime.now(VIETNAM_ZONE);
    }

    public static LocalDate todayInVietnam() {
        return LocalDate.now(VIETNAM_ZONE);
    }

    public static LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value);
    }

    public static long toVietnamEpochSeconds(LocalDateTime dateTime) {
        return dateTime.atZone(VIETNAM_ZONE).toEpochSecond();
    }

    public static LocalDateTime fromVietnamEpochSeconds(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), VIETNAM_ZONE);
    }
}

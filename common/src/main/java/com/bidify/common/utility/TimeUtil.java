package com.bidify.common.utility;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TimeUtil {
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

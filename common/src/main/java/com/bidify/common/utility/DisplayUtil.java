package com.bidify.common.utility;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class DisplayUtil {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US); // định dạng USD

    private DisplayUtil() {}

    public static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String formatCurrency(double amount) { // định dạng tiền tệ
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatRemainingTime(String endTime) { // định dạng thời gian còn lại
        if (endTime == null || endTime.isBlank()) return "Unknown";
        try {
            Duration duration = Duration.between(LocalDateTime.now(), LocalDateTime.parse(endTime));
            if (duration.isNegative() || duration.isZero()) return "Ended";
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        catch (DateTimeParseException e) {
            return endTime;
        }
    }

    public static String formatDateTime(String rawDate, String fallback) { // định dạng thời gian
        if (rawDate == null || rawDate.isBlank() || "Unknown".equals(rawDate)) return fallback;
        try {
            return LocalDateTime.parse(rawDate).toString().replace('T', ' ');
        }
        catch (DateTimeParseException e) {
            return rawDate;
        }
    }
}

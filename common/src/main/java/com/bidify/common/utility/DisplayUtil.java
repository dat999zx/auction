package com.bidify.common.utility;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class DisplayUtil {

    private static final NumberFormat CURRENCY_FORMAT =
            NumberFormat.getCurrencyInstance(Locale.US);

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        CURRENCY_FORMAT.setMinimumFractionDigits(2);
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
    }

    private DisplayUtil() {}

    public static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    public static String formatRemainingTime(String endTime) {
        if (endTime == null || endTime.isBlank()) {
            return "Unknown";
        }

        try {
            LocalDateTime end = LocalDateTime.parse(endTime);
            Duration duration = Duration.between(LocalDateTime.now(), end);

            if (duration.isNegative() || duration.isZero()) {
                return "Ended";
            }

            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();

            return String.format("%02d:%02d:%02d", hours, minutes, seconds);

        } catch (DateTimeParseException e) {
            return endTime;
        }
    }

    public static String formatDateTime(String rawDate, String fallback) {
        if (rawDate == null || rawDate.isBlank() || "Unknown".equals(rawDate)) {
            return fallback;
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(rawDate);
            return dateTime.format(DATE_TIME_FORMAT);

        } catch (DateTimeParseException e) {
            return rawDate;
        }
    }
}
package com.bidify.common.utility;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
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
            LocalDateTime end = TimeUtil.parseDateTime(endTime);
            Duration duration = Duration.between(TimeUtil.nowInVietnam(), end);

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
            LocalDateTime dateTime = TimeUtil.parseDateTime(rawDate);
            return dateTime.format(DATE_TIME_FORMAT);

        } catch (DateTimeParseException e) {
            return rawDate;
        }
    }



    /**
     * Formats a massive number into a suffix style 
     */

    private static final String[] SUFFIXES = {
        "", "k", "M", "B", "T", "qd", "Qn", "sx", "Sp", "O", "N",                                      // 0 - 10
        "de", "Ud", "DD", "tdD", "qdD", "QnD", "sxD", "SpD", "OcD", "NvD",                            // 11 - 20
        "Vgn", "UVg", "DVg", "TVg", "qtV", "QnV", "SeV", "SPG", "OVG", "NVG",                         // 21 - 30
        "TGN", "UTG", "DTG", "tsTG", "qtTG", "QnTG", "ssTG", "SpTG", "OcTG", "NoTG",                 // 31 - 40
        "QdDR", "uQDR", "dQDR", "tQDR", "qdQDR", "QnQDR", "sxQDR", "SpQDR", "OQDDr", "NQDDr",         // 41 - 50
        "qQGNT", "uQGNT", "dQGNT", "tQGNT", "qdQGNT", "QnQGNT", "sxQGNT", "SpQGNT", "OQQGNT", "NQQGNT", // 51 - 60
        "SXGNTL", "USXGNTL", "DSXGNTL", "TSXGNTL", "QTSXGNTL", "QNSXGNTL", "SXSXGNTL", "SPSXGNTL", "OSXGNTL", "NVSXGNTL", // 61 - 70
        "SPGNTL", "USPTGNTL", "DSPTGNTL", "TSPTGNTL", "QTSPTGNTL", "QNSPTGNTL", "SXSPTGNTL", "SPSPTGNTL", "OSPTGNTL", "NVSPTGNTL", // 71 - 80
        "OTGNTL", "UOTGNTL", "DOTGNTL", "TOTGNTL", "QTOTGNTL", "QNOTGNTL", "SXOTGNTL", "SPOTGNTL", "OTOTGNTL", "NVOTGNTL",     // 81 - 90
        "NONGNTL", "UNONGNTL", "DNONGNTL", "TNONGNTL", "QTNONGNTL", "QNNONGNTL", "SXNONGNTL", "SPNONGNTL", "OTNONGNTL", "NONONGNTL",          // 91 - 100
        "CENT", "UNCENT"
    };

    public static String formatCashSuffix(Double number) {
        BigDecimal bd = new BigDecimal(number);
        
        // 1. Handle base zero edge case safely
        if (bd.compareTo(BigDecimal.ZERO) == 0) {
            return "$0.00"; 
        }
        
        // 2. Track negativity and normalize to positive values for parsing
        boolean isNegative = bd.compareTo(BigDecimal.ZERO) < 0;
        bd = bd.abs();

        // Chuyển sang chuỗi số nguyên để tránh sai vị trí suffix khi double dùng dạng scientific notation.
        int totalDigits = bd.toBigInteger().toString().length();
        // Mỗi suffix đại diện cho một nhóm 3 chữ số.
        int suffixIndex = (totalDigits - 1) / 3;

        // 4. Over the limit check
        if (suffixIndex >= SUFFIXES.length) {
            return (isNegative ? "-$" : "+$") + "INF";
        }

        // 5. Shift the decimal point to read the localized short prefix
        BigDecimal divisor = BigDecimal.TEN.pow(suffixIndex * 3);
        BigDecimal shortValue = bd.divide(divisor, 4, RoundingMode.HALF_UP);

        // Giữ định dạng tối đa 3 chữ số để UI không bị dài bất thường.
        int leftSideDigits = shortValue.setScale(0, RoundingMode.DOWN).toString().length();
        DecimalFormat df;
        
        if (leftSideDigits == 1) {
            df = new DecimalFormat("0.00");  // e.g., 1.23
        } else if (leftSideDigits == 2) {
            df = new DecimalFormat("00.0");  // e.g., 12.3
        } else {
            df = new DecimalFormat("000");   // e.g., 123
        }
        
        df.setRoundingMode(RoundingMode.HALF_UP);
        String formattedNumber = df.format(shortValue);

        // 7. Combine parts back together
        return (isNegative ? "-$" : "$") + formattedNumber + SUFFIXES[suffixIndex];
    }
}

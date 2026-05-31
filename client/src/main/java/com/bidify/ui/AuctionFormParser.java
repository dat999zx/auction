package com.bidify.ui;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.bidify.common.exception.ValidationException;
import com.bidify.common.utility.TimeUtil;
import com.bidify.common.utility.ValidationUtil;
import javafx.scene.control.Label;

/**
 * Shared form-field parsing utilities for auction create/modify controllers.
 */
public class AuctionFormParser {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private AuctionFormParser() {}

    public static double parseAmount(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);
        try {
            double amount = Double.parseDouble(parseValue);
            if (amount < 0) throw new ValidationException(fieldName + " cannot be negative");
            return amount;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number");
        }
    }

    public static LocalTime parseTime(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);
        try {
            return LocalTime.parse(parseValue, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid format: " + fieldName + " must use HH:mm format (e.g., 09:30 or 23:59)");
        }
    }

    public static Duration parseDuration(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);
        if (!parseValue.matches("^\\d+:[0-5]\\d$")) {
            throw new ValidationException("Invalid format: " + fieldName + " must use H...H:mm format (e.g., 01:30 or 25:00)");
        }
        return TimeUtil.parseHHMM(parseValue);
    }

    public static void applyAntiSnipingState(Label label, AuctionAntiSnipingFormState state) {
        label.setText(state.labelText());
        label.setStyle(state.style());
    }
}

package com.bidify.server.utility;

import com.bidify.server.exceptions.ValidationException;

public class ValidationUtil {

    private ValidationUtil(){}

    public static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new ValidationException(fieldName + " cannot be empty"); 
    }

    public static void validateUsername(String username) {
        requireNonBlank(username, "Username");

        if (username.length() < 4 || username.length() > 20) throw new ValidationException("Username must be between 4 and 20 characters");
    }

    public static void validatePassword(String password) {
        requireNonBlank(password, "Password");

        if (password.length() < 6) throw new ValidationException("Password must be at least 6 characters");
    }

    public static void validatePhone(String phone) {
        requireNonBlank(phone, "Phone number");

        if (!phone.matches("\\d{10,11}")) throw new ValidationException("Phone number must contain 10 or 11 digits");
    }

    public static void validateEmail(String email) {
        if (email == null || email.isBlank()) return;

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) throw new ValidationException("Invalid email format");
    }

    public static void validatePositiveAmount(double amount, String fieldName) {
        if (amount <= 0) throw new ValidationException(fieldName + " must be greater than 0");
    }
}
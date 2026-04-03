package com.bidify.common.utility;

import com.bidify.common.exception.ValidationException;

public class ValidationUtil {
    private ValidationUtil(){}

    public static void requiresNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new ValidationException(fieldName + " cannot be empty"); 
    }

    public static void requiresNonSpace(String value, String fieldName){
        if (value == null || value.matches(".*\\s.*")) throw new ValidationException(fieldName + " cannot contains spaces");
    }

    public static void requiresNonBlank_Space(String value, String fieldName){
        requiresNonBlank(value, fieldName);
        requiresNonSpace(value, fieldName);
    }

    public static void validateUsername(String username) {
        requiresNonBlank(username, "Username");
        requiresNonSpace(username, "Username");

        if (username.length() < 4 || username.length() > 20) throw new ValidationException("Username must be between 4 and 20 characters");
    }

    public static void validateNickname(String nickname){
        requiresNonBlank(nickname, "Nickname");
        
        if (nickname.length() < 3 || nickname.length() > 20) throw new ValidationException("Nickname must be between 3 and 20 characters");
    }

    public static void validatePassword(String password) {
        requiresNonBlank(password, "Password");
        requiresNonSpace(password, "Password");

        if (password.length() < 6) throw new ValidationException("Password must be at least 6 characters");
    }

    public static void validatePhone(String phone) {
        requiresNonBlank(phone, "Phone number");

        if (!phone.matches("\\d{10,11}")) throw new ValidationException("Phone number must contain 10 or 11 digits");
    }

    public static void validateEmail(String email) {
        requiresNonBlank(email, "Email");
        requiresNonSpace(email, "Email");

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) throw new ValidationException("Invalid email format");
    }

    public static void greaterThan(double amount, double check, String fieldName) {
        if (amount <= check) throw new ValidationException(fieldName + " must be greater than " + amount);
    }

    public static void validatePositiveAmount(double amount, String fieldName) {
        greaterThan(amount, 0, fieldName);
    }

    public static void validateMinLength(String fieldName, String text, int minlength) {
        if (text.length() < minlength) throw new ValidationException(fieldName + "'s character length should be greater than " + minlength + " characters");
    }

    public static void validateMaxLength(String fieldName, String text, int maxlength) {
        if (text.length() > maxlength) throw new ValidationException(fieldName + "'s character length should be smaller than " + maxlength + " characters");
    }
}
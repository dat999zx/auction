package com.bidify.common.utility;

import com.bidify.common.exception.ValidationException;

public class ValidationUtil {
    // dùng để tạo một đối tượng ValidationUtil
    private ValidationUtil(){}

    // dùng để bắt buộc phải có non blank
    public static void requiresNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) throw new ValidationException(fieldName + " cannot be empty"); 
    }

    // dùng để bắt buộc phải có non space
    public static void requiresNonSpace(String value, String fieldName){
        if (value == null || value.matches(".*\\s.*")) throw new ValidationException(fieldName + " cannot contains spaces");
    }

    // dùng để bắt buộc phải có non blank_space
    public static void requiresNonBlank_Space(String value, String fieldName){
        // dùng để bắt buộc phải có non blank
        requiresNonBlank(value, fieldName);
        // dùng để bắt buộc phải có non space
        requiresNonSpace(value, fieldName);
    }

    // dùng để kiểm tra tính hợp lệ username
    public static void validateUsername(String username) {
        // dùng để bắt buộc phải có non blank
        requiresNonBlank(username, "Username");
        // dùng để bắt buộc phải có non space
        requiresNonSpace(username, "Username");

        if (username.length() < 4 || username.length() > 20) throw new ValidationException("Username must be between 4 and 20 characters");
    }

    // dùng để kiểm tra tính hợp lệ biệt danh
    public static void validateNickname(String nickname){
        // dùng để bắt buộc phải có non blank
        requiresNonBlank(nickname, "Nickname");
        
        if (nickname.length() < 3 || nickname.length() > 20) throw new ValidationException("Nickname must be between 3 and 20 characters");
    }

    // dùng để kiểm tra tính hợp lệ mật khẩu
    public static void validatePassword(String password) {
        // dùng để bắt buộc phải có non blank
        requiresNonBlank(password, "Password");
        // dùng để bắt buộc phải có non space
        requiresNonSpace(password, "Password");

        if (password.length() < 6) throw new ValidationException("Password must be at least 6 characters");
    }

    // dùng để kiểm tra tính hợp lệ phone
    public static void validatePhone(String phone) {
        // dùng để bắt buộc phải có non blank
        requiresNonBlank(phone, "Phone number");

        if (!phone.matches("\\d{10,11}")) throw new ValidationException("Phone number must contain 10 or 11 digits");
    }

    // dùng để kiểm tra tính hợp lệ email
    public static void validateEmail(String email) {
        // dùng để bắt buộc phải có non blank
        requiresNonBlank(email, "Email");
        // dùng để bắt buộc phải có non space
        requiresNonSpace(email, "Email");

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) throw new ValidationException("Invalid email format");
    }

    // dùng để greater than
    public static void greaterThan(double amount, double check, String fieldName) {
        if (amount <= check) throw new ValidationException(fieldName + " must be greater than " + amount);
    }

    // dùng để kiểm tra tính hợp lệ positive số tiền
    public static void validatePositiveAmount(double amount, String fieldName) {
        // dùng để greater than
        greaterThan(amount, 0, fieldName);
    }

    // dùng để kiểm tra tính hợp lệ min length
    public static void validateMinLength(String fieldName, String text, int minlength) {
        if (text.length() < minlength) throw new ValidationException(fieldName + "'s character length should be greater than " + minlength + " characters");
    }

    // dùng để kiểm tra tính hợp lệ max length
    public static void validateMaxLength(String fieldName, String text, int maxlength) {
        if (text.length() > maxlength) throw new ValidationException(fieldName + "'s character length should be smaller than " + maxlength + " characters");
    }
}

package com.bidify.common.utility;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.bidify.common.exception.ValidationException;

public class ValidationUtilTest {
    // dùng để valid username
    @Test
    void validUsername() { // username hợp lệ
        assertDoesNotThrow(() -> ValidationUtil.validateUsername("testUser"));
    }

    // dùng để username kiểm tra xem blank
    @Test
    void usernameIsBlank() { // username rỗng
        assertThrows(ValidationException.class, () -> ValidationUtil.validateUsername("  "));
    }

    // dùng để username contains spaces
    @Test
    void usernameContainsSpaces() { // username chứa khoảng trắng
        assertThrows(ValidationException.class, () -> ValidationUtil.validateUsername("test user"));
    }

    // dùng để nickname too short
    @Test
    void nicknameTooShort() { // nickname quá ngắn
        assertThrows(ValidationException.class, () -> ValidationUtil.validateNickname("ab"));
    }

    // dùng để password too short
    @Test
    void passwordTooShort() { // password quá ngắn
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePassword("12345"));
    }

    // dùng để amount kiểm tra xem zero or negative
    @Test
    void amountIsZeroOrNegative() { // amount <= 0
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePositiveAmount(0, "Bid amount"));
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePositiveAmount(-10, "Bid amount"));
    }

    // dùng để text too long
    @Test
    void textTooLong() { // text quá dài
        assertThrows(ValidationException.class, () -> ValidationUtil.validateMaxLength("Description", "123456", 5));
    }

    // dùng để valid email
    @Test
    void validEmail() { // email hợp lệ
        assertDoesNotThrow(() -> ValidationUtil.validateEmail("user@example.com"));
    }
}

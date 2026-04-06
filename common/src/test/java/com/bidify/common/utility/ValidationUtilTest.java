package com.bidify.common.utility;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.bidify.common.exception.ValidationException;

public class ValidationUtilTest {
    @Test
    void validUsername() { // username hợp lệ
        assertDoesNotThrow(() -> ValidationUtil.validateUsername("testUser"));
    }

    @Test
    void usernameIsBlank() { // username rỗng
        assertThrows(ValidationException.class, () -> ValidationUtil.validateUsername("  "));
    }

    @Test
    void usernameContainsSpaces() { // username chứa khoảng trắng
        assertThrows(ValidationException.class, () -> ValidationUtil.validateUsername("test user"));
    }

    @Test
    void nicknameTooShort() { // nickname quá ngắn
        assertThrows(ValidationException.class, () -> ValidationUtil.validateNickname("ab"));
    }

    @Test
    void passwordTooShort() { // password quá ngắn
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePassword("12345"));
    }

    @Test
    void amountIsZeroOrNegative() { // amount <= 0
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePositiveAmount(0, "Bid amount"));
        assertThrows(ValidationException.class, () -> ValidationUtil.validatePositiveAmount(-10, "Bid amount"));
    }

    @Test
    void textTooLong() { // text quá dài
        assertThrows(ValidationException.class, () -> ValidationUtil.validateMaxLength("Description", "123456", 5));
    }

    @Test
    void validEmail() { // email hợp lệ
        assertDoesNotThrow(() -> ValidationUtil.validateEmail("user@example.com"));
    }
}

package com.bidify.server.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.Response;
import com.bidify.common.exception.ValidationException;
import com.bidify.server.exception.DatabaseException;

class ServiceUtilTest {

    @Test
    void handleRequestWithValidationExceptionReturnsMessage() {
        Response response = ServiceUtil.handleRequest(() -> {
            throw new ValidationException("User validation failed");
        });
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("User validation failed", response.getMessage());
    }

    @Test
    void handleRequestWithDatabaseExceptionReturnsMessage() {
        Response response = ServiceUtil.handleRequest(() -> {
            throw new DatabaseException("Connection timed out");
        });
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Connection timed out", response.getMessage());
    }

    @Test
    void handleRequestWithUnexpectedRuntimeExceptionSanitizesMessage() {
        Response response = ServiceUtil.handleRequest(() -> {
            throw new RuntimeException("Sensitive database credentials leaked!");
        });
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Internal server error", response.getMessage());
    }
}

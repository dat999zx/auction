package com.bidify.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AuctionAntiSnipingFormStateTest {
    @Test
    void enabledWhenMaxEndIsAfterStandardEnd() {
        AuctionAntiSnipingFormState state = AuctionAntiSnipingFormState.from(
                LocalDateTime.of(2026, 5, 24, 10, 0),
                LocalDateTime.of(2026, 5, 24, 10, 5));

        assertTrue(state.enabled());
        assertEquals("Anti-Sniping Status: Enabled", state.labelText());
        assertEquals("-fx-text-fill: #2ecc71; -fx-font-weight: bold;", state.style());
    }

    @Test
    void disabledWhenMaxEndEqualsStandardEnd() {
        AuctionAntiSnipingFormState state = AuctionAntiSnipingFormState.from(
                LocalDateTime.of(2026, 5, 24, 10, 0),
                LocalDateTime.of(2026, 5, 24, 10, 0));

        assertFalse(state.enabled());
        assertEquals("Anti-Sniping Status: Disabled", state.labelText());
        assertEquals("-fx-text-fill: #e74c3c; -fx-font-weight: bold;", state.style());
    }

    @Test
    void invalidWhenMaxEndIsBeforeStandardEnd() {
        AuctionAntiSnipingFormState state = AuctionAntiSnipingFormState.from(
                LocalDateTime.of(2026, 5, 24, 10, 0),
                LocalDateTime.of(2026, 5, 24, 9, 59));

        assertFalse(state.enabled());
        assertTrue(state.maxEndBeforeStandardEnd());
    }
}

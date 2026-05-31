package com.bidify.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalTime;
import java.time.LocalDateTime;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.bidify.common.exception.ValidationException;

/**
 * Các ca kiểm thử cho AuctionFormParser trong module client.
 */
class AuctionFormParserTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            // Khởi chạy JavaFX Platform để có thể khởi tạo đối tượng Label mà không bị lỗi Toolkit
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Bỏ qua nếu nền tảng JavaFX đã được chạy trước đó
        }
    }

    @Test
    void testParseAmountValid() {
        // Parse số tiền hợp lệ
        assertEquals(100.5, AuctionFormParser.parseAmount("100.5", "price"));
        assertEquals(0.0, AuctionFormParser.parseAmount(" 0 ", "price"));
    }

    @Test
    void testParseAmountNullOrBlankThrows() {
        // Số tiền null hoặc trống
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseAmount(null, "price"));
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseAmount("   ", "price"));
    }

    @Test
    void testParseAmountNegativeThrows() {
        // Số tiền âm
        ValidationException ex = assertThrows(ValidationException.class, 
                () -> AuctionFormParser.parseAmount("-10.0", "price"));
        assertTrue(ex.getMessage().contains("cannot be negative"));
    }

    @Test
    void testParseAmountInvalidNumberThrows() {
        // Chuỗi không phải là số
        ValidationException ex = assertThrows(ValidationException.class, 
                () -> AuctionFormParser.parseAmount("not-a-number", "price"));
        assertTrue(ex.getMessage().contains("must be a valid number"));
    }

    @Test
    void testParseTimeValid() {
        // Parse thời gian HH:mm hợp lệ
        LocalTime time = AuctionFormParser.parseTime("14:30", "startTime");
        assertEquals(LocalTime.of(14, 30), time);
    }

    @Test
    void testParseTimeNullOrBlankThrows() {
        // Thời gian null hoặc trống
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseTime(null, "startTime"));
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseTime("  ", "startTime"));
    }

    @Test
    void testParseTimeInvalidFormatThrows() {
        // Sai định dạng thời gian
        ValidationException ex = assertThrows(ValidationException.class, 
                () -> AuctionFormParser.parseTime("2:30", "startTime"));
        assertTrue(ex.getMessage().contains("must use HH:mm format"));
        
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseTime("14-30", "startTime"));
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseTime("invalid", "startTime"));
    }

    @Test
    void testParseDurationValid() {
        // Parse thời gian kéo dài hợp lệ (H...H:mm)
        Duration duration = AuctionFormParser.parseDuration("01:30", "duration");
        assertEquals(Duration.ofHours(1).plusMinutes(30), duration);

        Duration multiHour = AuctionFormParser.parseDuration("125:45", "duration");
        assertEquals(Duration.ofHours(125).plusMinutes(45), multiHour);
    }

    @Test
    void testParseDurationNullOrBlankThrows() {
        // Thời lượng null hoặc trống
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseDuration(null, "duration"));
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseDuration(" ", "duration"));
    }

    @Test
    void testParseDurationInvalidFormatThrows() {
        // Sai định dạng duration
        ValidationException ex = assertThrows(ValidationException.class, 
                () -> AuctionFormParser.parseDuration("abc", "duration"));
        assertTrue(ex.getMessage().contains("must use H...H:mm format"));

        assertThrows(ValidationException.class, () -> AuctionFormParser.parseDuration("01:60", "duration")); // phút > 59
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseDuration("01:0", "duration"));  // phút thiếu số
        assertThrows(ValidationException.class, () -> AuctionFormParser.parseDuration(":30", "duration"));   // thiếu giờ
    }

    @Test
    void testApplyAntiSnipingState() {
        // Tạo label và áp dụng trạng thái anti-sniping
        Label label = new Label();
        AuctionAntiSnipingFormState state = AuctionAntiSnipingFormState.from(
                LocalDateTime.of(2026, 5, 24, 10, 0),
                LocalDateTime.of(2026, 5, 24, 10, 5));

        AuctionFormParser.applyAntiSnipingState(label, state);

        assertEquals("Anti-Sniping Status: Enabled", label.getText());
        assertEquals("-fx-text-fill: #2ecc71; -fx-font-weight: bold;", label.getStyle());
    }
}

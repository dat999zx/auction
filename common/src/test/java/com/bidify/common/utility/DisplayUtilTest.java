package com.bidify.common.utility;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Các ca kiểm thử cho DisplayUtil thuộc module common.
 */
class DisplayUtilTest {

    @Test
    void testDefaultText() {
        // Kiểm tra trả về giá trị fallback khi chuỗi đầu vào bị null, trống hoặc trắng
        assertEquals("fallback", DisplayUtil.defaultText(null, "fallback"));
        assertEquals("fallback", DisplayUtil.defaultText("", "fallback"));
        assertEquals("fallback", DisplayUtil.defaultText("   ", "fallback"));
        assertEquals("real-value", DisplayUtil.defaultText("real-value", "fallback"));
    }

    @Test
    void testFormatCurrency() {
        // Định dạng tiền tệ với Locale.US mặc định
        String formatted = DisplayUtil.formatCurrency(1234.56);
        // Vì có thể có ký tự khoảng trắng không ngắt (non-breaking space) hoặc ký tự đặc biệt tùy môi trường,
        // chúng ta sẽ kiểm tra xem nó có chứa ký tự '$' và số tiền '1,234.56' hay không.
        assertTrue(formatted.contains("$"));
        assertTrue(formatted.contains("1,234.56"));
    }

    @Test
    void testFormatRemainingTimeNullOrBlank() {
        // Đầu vào null hoặc rỗng trả về "Unknown"
        assertEquals("Unknown", DisplayUtil.formatRemainingTime(null));
        assertEquals("Unknown", DisplayUtil.formatRemainingTime(""));
        assertEquals("Unknown", DisplayUtil.formatRemainingTime("   "));
    }

    @Test
    void testFormatRemainingTimeInvalidFormat() {
        // Định dạng ngày giờ không hợp lệ trả về nguyên chuỗi đầu vào
        assertEquals("invalid-date-format", DisplayUtil.formatRemainingTime("invalid-date-format"));
    }

    @Test
    void testFormatRemainingTimeEnded() {
        // Thời gian kết thúc ở quá khứ hoặc hiện tại trả về "Ended"
        String pastTime = TimeUtil.nowInVietnam().minusMinutes(5).toString();
        assertEquals("Ended", DisplayUtil.formatRemainingTime(pastTime));
    }

    @Test
    void testFormatRemainingTimePositiveFuture() {
        // Thời gian ở tương lai trả về dạng HH:mm:ss
        LocalDateTime futureTime = TimeUtil.nowInVietnam().plusHours(2).plusMinutes(15).plusSeconds(30);
        String formatted = DisplayUtil.formatRemainingTime(futureTime.toString());
        
        // Để tránh sai lệch vài mili giây khi chạy test, chúng ta kiểm tra format và giờ/phút tương đối
        // Định dạng phải khớp regex dạng HH:mm:ss (ví dụ "02:15:30" hoặc "02:15:29")
        assertTrue(formatted.matches("^02:15:\\d{2}$"), 
                "Thời gian hiển thị không đúng định dạng HH:mm:ss hoặc sai giờ phút: " + formatted);
    }

    @Test
    void testFormatDateTimeNullOrBlankOrUnknown() {
        // Đầu vào null, rỗng hoặc "Unknown" trả về fallback
        assertEquals("my-fallback", DisplayUtil.formatDateTime(null, "my-fallback"));
        assertEquals("my-fallback", DisplayUtil.formatDateTime("", "my-fallback"));
        assertEquals("my-fallback", DisplayUtil.formatDateTime("   ", "my-fallback"));
        assertEquals("my-fallback", DisplayUtil.formatDateTime("Unknown", "my-fallback"));
    }

    @Test
    void testFormatDateTimeValid() {
        // Định dạng ngày giờ hợp lệ
        String raw = "2026-05-24T10:15:30";
        String formatted = DisplayUtil.formatDateTime(raw, "fallback");
        assertEquals("2026-05-24 10:15:30", formatted);
    }

    @Test
    void testFormatDateTimeInvalid() {
        // Nếu không parse được thì trả về nguyên bản
        assertEquals("not-a-datetime", DisplayUtil.formatDateTime("not-a-datetime", "fallback"));
    }

    @Test
    void testFormatCashSuffixZero() {
        // Trường hợp số bằng 0
        assertEquals("$0.00", DisplayUtil.formatCashSuffix(0.0));
    }

    @Test
    void testFormatCashSuffixPositive() {
        // Định dạng các số dương với các suffix tương ứng
        assertEquals("$1.00", DisplayUtil.formatCashSuffix(1.0));
        assertEquals("$15.0", DisplayUtil.formatCashSuffix(15.0));
        assertEquals("$150", DisplayUtil.formatCashSuffix(150.0));
        
        assertEquals("$1.50k", DisplayUtil.formatCashSuffix(1500.0));
        assertEquals("$15.0k", DisplayUtil.formatCashSuffix(15000.0));
        assertEquals("$150k", DisplayUtil.formatCashSuffix(150000.0));
        
        assertEquals("$1.50M", DisplayUtil.formatCashSuffix(1500000.0));
        assertEquals("$12.3M", DisplayUtil.formatCashSuffix(12345678.9));
        assertEquals("$999M", DisplayUtil.formatCashSuffix(999000000.0));
        
        assertEquals("$1.00B", DisplayUtil.formatCashSuffix(1000000000.0));
    }

    @Test
    void testFormatCashSuffixNegative() {
        // Định dạng các số âm
        assertEquals("-$1.50k", DisplayUtil.formatCashSuffix(-1500.0));
        assertEquals("-$12.3M", DisplayUtil.formatCashSuffix(-12345678.9));
    }
}

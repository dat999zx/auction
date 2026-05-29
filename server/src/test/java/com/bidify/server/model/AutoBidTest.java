package com.bidify.server.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn giản cho AutoBid.
 */
public class AutoBidTest {

    @Test
    public void testAutoBidInitializationAndDisabling() {
        String auctionId = "auc-123";
        String username = "user_auto";
        double maxBid = 5000.0;

        // Khởi tạo AutoBid
        AutoBid autoBid = new AutoBid(auctionId, username, maxBid);

        // Xác thực các thuộc tính ban đầu
        assertEquals(auctionId, autoBid.getAuctionId());
        assertEquals(username, autoBid.getUsername());
        assertEquals(maxBid, autoBid.getMaxBid());
        assertTrue(autoBid.isEnabled(), "Mặc định auto-bid phải được bật");
        assertNotNull(autoBid.getCreatedAt(), "Thời điểm tạo không được null");

        // Thay đổi giá tối đa
        autoBid.setMaxBid(6000.0);
        assertEquals(6000.0, autoBid.getMaxBid());

        // Kiểm tra vô hiệu hóa
        autoBid.disable();
        assertFalse(autoBid.isEnabled(), "Auto-bid phải bị tắt sau khi gọi disable()");
    }
}

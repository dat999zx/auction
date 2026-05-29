package com.bidify.server.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn giản cho Bid.
 */
public class BidTest {

    @Test
    public void testBidInitializationWithDefaultConstructor() {
        String auctionId = "auc-456";
        String bidder = "bidder_usr";
        double amount = 1500.0;

        // Khởi động constructor mặc định (autoBidGenerated = false)
        Bid bid = new Bid(auctionId, bidder, amount);

        // Xác thực
        assertNotNull(bid.getId(), "ID phải được tự động sinh");
        assertNotNull(bid.getCreatedAt(), "Thời gian tạo phải được tự động gán");
        assertEquals(auctionId, bid.getAuctionId());
        assertEquals(bidder, bid.getBidderUsername());
        assertEquals(amount, bid.getAmount());
        assertFalse(bid.isAutoBidGenerated(), "Mặc định autoBidGenerated phải là false");
    }

    @Test
    public void testBidInitializationWithFullConstructor() {
        String auctionId = "auc-456";
        String bidder = "bidder_usr";
        double amount = 1500.0;

        // Khởi động constructor đầy đủ
        Bid bid = new Bid(auctionId, bidder, amount, true);

        // Xác thực
        assertEquals(auctionId, bid.getAuctionId());
        assertEquals(bidder, bid.getBidderUsername());
        assertEquals(amount, bid.getAmount());
        assertTrue(bid.isAutoBidGenerated(), "autoBidGenerated phải khớp với tham số truyền vào");
    }

    @Test
    public void testBidInitializationFromDatabaseConstructor() {
        String id = "bid-custom-id";
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 28, 12, 0, 0);
        String auctionId = "auc-789";
        String bidder = "db_bidder";
        double amount = 2500.0;

        // Constructor giả lập nạp từ database
        Bid bid = new Bid(id, createdAt, auctionId, bidder, amount, true);

        // Xác thực bảo toàn thuộc tính từ DB
        assertEquals(id, bid.getId(), "ID phải khớp với ID truyền vào từ DB");
        assertEquals(createdAt, bid.getCreatedAt(), "CreatedAt phải khớp với thời gian truyền vào từ DB");
        assertEquals(auctionId, bid.getAuctionId());
        assertEquals(bidder, bid.getBidderUsername());
        assertEquals(amount, bid.getAmount());
        assertTrue(bid.isAutoBidGenerated());
    }
}

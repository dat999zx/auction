package com.bidify.server.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.bidify.common.enums.AuctionStatus;

public class AuctionTest {
    @Test
    void updateAuctionAfterPlaceBidSuccessfully() { // cập nhật auction sau khi đặt bid thành công
        Auction auction = new Auction("seller", "test", "testing auction", 1000, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        auction.setMinIncrement(100);

        Bid bid = new Bid(auction, "user1", 1100);

        assertTrue(auction.placeBid(bid));
        assertEquals(1100, auction.getCurrentBid());
        assertEquals("user1", auction.getCurrentBidderUsername());
        assertEquals(1, auction.getBidCount());
    }

    @Test
    void placeBidLowerThanMinIncrement() { // đặt bid thấp hơn min increment
        Auction auction = new Auction("seller", "test", "testing auction", 1000, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        auction.setMinIncrement(100);

        Bid bid = new Bid(auction, "user1", 1050);

        assertFalse(auction.placeBid(bid));
        assertEquals(0, auction.getCurrentBid());
        assertNull(auction.getCurrentBidderUsername());
        assertEquals(0, auction.getBidCount());
    }

    @Test
    void placeBidWhenAuctionIsNotActive() { // đặt bid khi auction đang ko live
        Auction auction = new Auction("seller", "test", "testing auction", 1000, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        auction.setStatus(AuctionStatus.ENDED);

        Bid bid = new Bid(auction, "user1", 1200);

        assertFalse(auction.placeBid(bid));
        assertEquals(0, auction.getCurrentBid());
        assertNull(auction.getCurrentBidderUsername());
        assertEquals(0, auction.getBidCount());
    }

    @Test
    void handleConcurrentBidSafely() throws Exception { // test nhiều người đặt bid cùng lúc
        Auction auction = new Auction("seller", "test", "testing auction", 1000, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        int n_threads = 3; // số lượng thread muốn test
        
        ExecutorService executor = Executors.newFixedThreadPool(n_threads); // tạo n_threads bid cùng lúc
        CountDownLatch ready = new CountDownLatch(n_threads); // chờ tất cả thread sẵn sàng
        CountDownLatch start = new CountDownLatch(1); // mở cổng, tất cả vào chung 1 cổng này cùng lúc để chạy song song
        CountDownLatch done = new CountDownLatch(n_threads); // kết thúc tất cả thread

        double[] amounts = {1100, 1200, 1050};

        // khởi tạo các thread sẵn sàng để chạy song song
        for (int i = 0; i < n_threads; i++){
            final int idx = i;
            executor.submit(() -> {
                try {
                    Bid bid = new Bid(auction, "user" + idx, amounts[idx]);

                    ready.countDown(); // thread sẵn sàng
                    start.await(); // chờ lệnh xuất phát

                    auction.placeBid(bid);
                }
                catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                }
                finally {
                    done.countDown(); // kết thúc
                }
            });
        }

        ready.await(); // chờ sẵn sàng
        start.countDown(); // bắt đầu
        done.await(); // chờ tất cả kết thúc

        assertEquals(1200, auction.getCurrentBid()); // bid cao nhất
        assertNotNull(auction.getCurrentBidderUsername());
        assertEquals("user1", auction.getCurrentBidderUsername()); // người đặt cao nhất
        executor.shutdown(); // đóng thread
    }
}

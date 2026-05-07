package com.bidify.server.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;

class AuctionDaoTest {
    private AuctionDao auctionDao = AuctionDao.getInstance();
    private String testAuctionId;

    @BeforeAll
    static void initDatabase() {
        // Khởi tạo SQLite schema trước khi chạy tất cả tests
        SQLiteHelper.init();
    }

    @BeforeEach
    void setUp() throws Exception {
        // Tạo auction test trước mỗi test case
        Auction auction = new Auction(
            "Test Auction",
            "Test Description",
            "seller",
            1000.0,
            LocalDateTime.now().plusMinutes(1),
            LocalDateTime.now().plusHours(1)
        );
        auctionDao.create(auction);
        testAuctionId = auction.getId();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Dọn dẹp auction test sau mỗi test case
        if (testAuctionId != null) {
            auctionDao.deleteById(testAuctionId);
        }
    }

    @Test
    void createAndFindAuctionSuccessfully() {
        // Auction đã được tạo trong setUp()

        // Tìm auction theo ID
        Auction found = auctionDao.findById(testAuctionId);

        // Kiểm tra auction được tìm thấy và có dữ liệu đúng
        // - Không null (tức là tìm thấy)
        // - Tên auction khớp với dữ liệu đã tạo
        // - Seller username khớp
        assertNotNull(found);
        assertEquals("Test Auction", found.getAuctionName());
        assertEquals("seller", found.getSellerUsername());
        assertEquals(1000.0, found.getStartingPrice());
    }

    @Test
    void findAuctionByIdReturnsNullWhenNotFound() {
        // ID không tồn tại trong DB
        String invalidId = "non-existent-id";

        // Tìm auction với ID không tồn tại
        Auction found = auctionDao.findById(invalidId);

        // Kết quả phải null (không tìm thấy)
        assertNull(found);
    }

    @Test
    void updateAuctionSuccessfully() {
        // Lấy auction hiện tại từ DB
        Auction auction = auctionDao.findById(testAuctionId);
        assertNotNull(auction);

        // Cập nhật tên auction và lưu vào DB
        auction.setAuctionName("Updated Auction Name");
        auction.setDescription("Updated Description");
        auctionDao.save(auction);

        // Đọc lại từ DB và kiểm tra dữ liệu đã được cập nhật
        Auction updated = auctionDao.findById(testAuctionId);
        assertEquals("Updated Auction Name", updated.getAuctionName());
        assertEquals("Updated Description", updated.getDescription());
    }

    @Test
    void deleteAuctionByIdSuccessfully() {
        // Auction tồn tại trong DB (từ setUp)
        Auction beforeDelete = auctionDao.findById(testAuctionId);
        assertNotNull(beforeDelete);

        // Xóa auction theo ID
        auctionDao.deleteById(testAuctionId);

        // Auction không còn tồn tại trong DB
        Auction afterDelete = auctionDao.findById(testAuctionId);
        assertNull(afterDelete);

        // Đánh dấu để tearDown không xóa lại
        testAuctionId = null;
    }

    @Test
    void findAuctionsByStatusReturnsCorrectList() throws Exception {
        // Tạo thêm auction với status khác nhau
        Auction activeAuction = new Auction(
            "Active Auction",
            "Active Description",
            "seller2",
            500.0,
            LocalDateTime.now().minusMinutes(1), // Đã bắt đầu
            LocalDateTime.now().plusHours(1)
        );
        auctionDao.create(activeAuction);

        // Tìm auctions theo status UPCOMING
        List<Auction> upcomingAuctions = auctionDao.findByStatus(AuctionStatus.UPCOMING);

        // Danh sách phải chứa ít nhất auction test (UPCOMING)
        assertTrue(upcomingAuctions.size() >= 1);
        boolean foundTestAuction = upcomingAuctions.stream()
            .anyMatch(a -> a.getId().equals(testAuctionId));
        assertTrue(foundTestAuction);

        // Cleanup
        auctionDao.deleteById(activeAuction.getId());
    }
}
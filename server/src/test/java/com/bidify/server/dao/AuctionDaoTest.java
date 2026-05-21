package com.bidify.server.dao;

import java.time.LocalDateTime;
import com.bidify.common.utility.TimeUtil;
import java.util.ArrayList;
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
import com.bidify.server.model.Item;

class AuctionDaoTest {
    private AuctionDao auctionDao = AuctionDao.getInstance();
    private ItemDao itemDao = ItemDao.getInstance();
    private String testAuctionId;
    private final List<String> createdItemIds = new ArrayList<>();

    // dùng để khởi tạo cơ sở dữ liệu
    @BeforeAll
    static void initDatabase() {
        // Khởi tạo SQLite schema trước khi chạy tất cả tests
        SQLiteHelper.init();
    }

    // dùng để thiết lập up
    @BeforeEach
    void setUp() throws Exception {
        // Tạo auction test trước mỗi test case
        Auction auction = new Auction(
            "Test Auction",
            "Test Description",
            "seller",
            1000.0,
            TimeUtil.nowInVietnam().plusMinutes(1),
            TimeUtil.nowInVietnam().plusHours(1)
        );
        Item item = createItem("seller", "Test Auction Item");
        auction.setItemId(item.getId());
        auctionDao.create(auction);
        testAuctionId = auction.getId();
    }

    // dùng để tear down
    @AfterEach
    void tearDown() throws Exception {
        // Dọn dẹp auction test sau mỗi test case
        if (testAuctionId != null) {
            auctionDao.deleteById(testAuctionId);
        }
        for (String itemId : createdItemIds)
            itemDao.deleteById(itemId);
        createdItemIds.clear();
    }

    // dùng để tạo and tìm kiếm đấu giá successfully
    @Test
    void createAndFindAuctionSuccessfully() {
        // Auction đã được tạo trong setUp()

        // Tìm auction theo ID
        Auction found = auctionDao.findById(testAuctionId);

        // Kiểm tra auction được tìm thấy và có dữ liệu đúng
        // - Không null (tức là tìm thấy)
        // - Tên auction khớp với dữ liệu đã tạo
        // - Seller username khớp
        // dùng để assert not null
        assertNotNull(found);
        assertEquals("Test Auction", found.getAuctionName());
        assertEquals("seller", found.getSellerUsername());
        assertEquals(1000.0, found.getStartingPrice());
    }

    // dùng để tìm kiếm đấu giá bởi ID returns null when not found
    @Test
    void findAuctionByIdReturnsNullWhenNotFound() {
        // ID không tồn tại trong DB
        String invalidId = "non-existent-id";

        // Tìm auction với ID không tồn tại
        Auction found = auctionDao.findById(invalidId);

        // Kết quả phải null (không tìm thấy)
        // dùng để assert null
        assertNull(found);
    }

    // dùng để cập nhật đấu giá successfully
    @Test
    void updateAuctionSuccessfully() {
        // Lấy auction hiện tại từ DB
        Auction auction = auctionDao.findById(testAuctionId);
        // dùng để assert not null
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

    // dùng để xóa đấu giá bởi ID successfully
    @Test
    void deleteAuctionByIdSuccessfully() {
        // Auction tồn tại trong DB (từ setUp)
        Auction beforeDelete = auctionDao.findById(testAuctionId);
        // dùng để assert not null
        assertNotNull(beforeDelete);

        // Xóa auction theo ID
        auctionDao.deleteById(testAuctionId);

        // Auction không còn tồn tại trong DB
        Auction afterDelete = auctionDao.findById(testAuctionId);
        // dùng để assert null
        assertNull(afterDelete);

        // Đánh dấu để tearDown không xóa lại
        testAuctionId = null;
    }

    // dùng để tìm kiếm danh sách đấu giá bởi trạng thái returns correct list
    @Test
    void findAuctionsByStatusReturnsCorrectList() throws Exception {
        // Tạo thêm auction với status khác nhau
        Auction activeAuction = new Auction(
            "Active Auction",
            "Active Description",
            "seller2",
            500.0,
            TimeUtil.nowInVietnam().minusMinutes(1), // Đã bắt đầu
            TimeUtil.nowInVietnam().plusHours(1)
        );
        Item item = createItem("seller2", "Active Auction Item");
        activeAuction.setItemId(item.getId());
        auctionDao.create(activeAuction);

        // Tìm auctions theo status UPCOMING
        List<Auction> upcomingAuctions = auctionDao.findByStatus(AuctionStatus.UPCOMING);

        // Danh sách phải chứa ít nhất auction test (UPCOMING)
        assertTrue(upcomingAuctions.size() >= 1);
        boolean foundTestAuction = upcomingAuctions.stream()
            .anyMatch(a -> a.getId().equals(testAuctionId));
        // dùng để assert true
        assertTrue(foundTestAuction);

        // Cleanup
        auctionDao.deleteById(activeAuction.getId());
    }
    // dùng để tạo sản phẩm
    private Item createItem(String ownerUsername, String name) {
        Item item = new Item(ownerUsername, name, "Test Description", "General", "Electronics");
        itemDao.create(item);
        createdItemIds.add(item.getId());
        return item;
    }
}

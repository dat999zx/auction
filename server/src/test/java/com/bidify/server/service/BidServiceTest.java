package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.dto.BidDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.Response;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) cho {@link BidService}.
 * Kiểm tra các tính năng:
 * - Lấy lịch sử đặt giá (Bid History) thành công khi cuộc đấu giá đang chạy thực tế (Runtime).
 * - Lấy lịch sử đặt giá thành công khi cuộc đấu giá đã kết thúc/hủy và lưu trong SQLite Database.
 * - Lấy lịch sử đặt giá trả về danh sách trống khi người dùng chưa từng đặt giá.
 * - Kiểm tra lỗi phân quyền nếu phiên làm việc của client không hợp lệ (Chưa đăng nhập).
 * Có chú thích chi tiết bằng tiếng Việt.
 */
class BidServiceTest {

    private final BidService bidService = BidService.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    private final List<String> createdUsernames = new ArrayList<>();
    private final List<String> createdItemIds = new ArrayList<>();
    private final List<String> createdAuctionIds = new ArrayList<>();
    private final List<String> createdBidIds = new ArrayList<>();

    @BeforeAll
    static void initDatabase() {
        SQLiteHelper.init();
    }

    @BeforeEach
    void setUp() {
        RealtimeDatabase.clearAll();
        createdUsernames.clear();
        createdItemIds.clear();
        createdAuctionIds.clear();
        createdBidIds.clear();
    }

    @AfterEach
    void tearDown() {
        RealtimeDatabase.clearAll();

        // Xóa Bids thủ công để tránh rò rỉ khóa ngoại
        for (String bidId : createdBidIds) {
            SQLiteHelper.update("DELETE FROM Bids WHERE id = ?", bidId);
        }

        // Xóa Bids và Transactions liên quan tới Auctions
        for (String auctionId : createdAuctionIds) {
            SQLiteHelper.update("DELETE FROM Bids WHERE auctionId = ?", auctionId);
            SQLiteHelper.update("DELETE FROM Transactions WHERE auctionId = ?", auctionId);
            SQLiteHelper.update("DELETE FROM Auctions WHERE id = ?", auctionId);
        }

        // Xóa các sản phẩm
        for (String itemId : createdItemIds) {
            SQLiteHelper.update("DELETE FROM ItemImageLinks WHERE itemId = ?", itemId);
            SQLiteHelper.update("DELETE FROM Items WHERE id = ?", itemId);
        }

        // Xóa các user
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }
    }

    /**
     * Ca kiểm thử: Người dùng lấy lịch sử đặt giá thành công khi cuộc đấu giá đang chạy thực tế (Runtime).
     * Xác thực: Dữ liệu cuộc đấu giá được tải thành công từ RealtimeDatabase.
     */
    @Test
    void getUserBidsSuccessfullyWhenAuctionIsRuntime() {
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createTestUser(bidderUsername);

        // Tạo cuộc đấu giá đang ACTIVE và lưu vào Runtime
        Auction auction = createTestAuction("Active Auction", AuctionStatus.ACTIVE);
        RealtimeDatabase.addRuntimeAuction(auction);

        // Tạo lượt đặt giá của user lên cuộc đấu giá này
        Bid bid = new Bid(auction.getId(), bidderUsername, 1200.0, false);
        bidDao.create(bid);
        createdBidIds.add(bid.getId());

        TestClientHandler client = sessionClient(bidder);

        Response response = bidService.getUserBids(client);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Bid history loaded", response.getMessage());

        assertNotNull(response.getData());
        List<BidDto> bids = (List<BidDto>) response.getData();
        assertFalse(bids.isEmpty());

        BidDto dto = bids.get(0);
        assertEquals(bid.getId(), dto.getId());
        assertEquals(auction.getId(), dto.getAuctionId());
        assertEquals(1200.0, dto.getAmount());
        assertFalse(dto.isAutoBidGenerated());
    }

    /**
     * Ca kiểm thử: Người dùng lấy lịch sử đặt giá thành công khi cuộc đấu giá đã kết thúc/hủy và chỉ lưu trong DB.
     * Xác thực: Dữ liệu cuộc đấu giá được tải thành công từ SQLite Database (auctionDao).
     */
    @Test
    void getUserBidsSuccessfullyWhenAuctionIsInDatabase() {
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createTestUser(bidderUsername);

        // Tạo cuộc đấu giá đã kết thúc (CANCELED) và KHÔNG lưu vào Runtime
        Auction auction = createTestAuction("Ended Auction", AuctionStatus.CANCELED);

        // Tạo lượt đặt giá của user
        Bid bid = new Bid(auction.getId(), bidderUsername, 1500.0, true);
        bidDao.create(bid);
        createdBidIds.add(bid.getId());

        TestClientHandler client = sessionClient(bidder);

        Response response = bidService.getUserBids(client);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Bid history loaded", response.getMessage());

        assertNotNull(response.getData());
        List<BidDto> bids = (List<BidDto>) response.getData();
        assertFalse(bids.isEmpty());

        BidDto dto = bids.get(0);
        assertEquals(bid.getId(), dto.getId());
        assertEquals(auction.getId(), dto.getAuctionId());
        assertEquals(1500.0, dto.getAmount());
        assertTrue(dto.isAutoBidGenerated());
    }

    /**
     * Ca kiểm thử: Người dùng mới chưa từng đặt giá sẽ nhận danh sách trống.
     */
    @Test
    void getUserBidsSuccessfullyReturnsEmptyList() {
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createTestUser(bidderUsername);

        TestClientHandler client = sessionClient(bidder);

        Response response = bidService.getUserBids(client);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Bid history loaded", response.getMessage());

        assertNotNull(response.getData());
        List<BidDto> bids = (List<BidDto>) response.getData();
        assertTrue(bids.isEmpty(), "Người dùng chưa đặt giá phải nhận danh sách trống.");
    }

    /**
     * Ca kiểm thử: Thất bại khi lấy lịch sử đặt giá do chưa đăng nhập (Invalid session).
     */
    @Test
    void getUserBidsFailsWhenNotLoggedIn() {
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(null); // Chưa đăng nhập

        Response response = bidService.getUserBids(client);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Invalid session", response.getMessage());
    }

    // --- Hàm trợ giúp (Helper Methods) ---

    private User createTestUser(String username) {
        User user = new User(username, username, PasswordUtil.hash("secret123"));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    private Auction createTestAuction(String name, AuctionStatus status) {
        String seller = uniqueUsername("seller");
        createTestUser(seller);

        Item item = new Item(seller, name, "Mô tả sản phẩm test", "CategoryTest", "TypeTest");
        itemDao.create(item);
        createdItemIds.add(item.getId());

        Auction auction = new Auction(
            seller, 
            item.getId(), 
            1000.0, 
            TimeUtil.nowInVietnam().minusHours(1),
            TimeUtil.nowInVietnam().plusHours(1)
        );
        auction.setAuctionName(name);
        auction.setDescription("Mô tả đấu giá test");
        auction.setMinIncrement(100.0);
        auction.setStatus(status);
        auctionDao.create(auction);
        createdAuctionIds.add(auction.getId());
        return auction;
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private TestClientHandler sessionClient(User user) {
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(user.getUsername());
        RealtimeDatabase.addActiveUser(client, user);
        return client;
    }

    // --- Mock Client Handler ---

    private static class TestClientHandler extends ClientHandler {
        TestClientHandler() {
            super(null);
        }

        @Override
        public boolean isInSession() {
            return getCurrentUsername() != null;
        }
    }
}

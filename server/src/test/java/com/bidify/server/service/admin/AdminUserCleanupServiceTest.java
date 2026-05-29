package com.bidify.server.service.admin;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;
import com.bidify.common.model.Event;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.AuctionService;
import com.bidify.server.service.ImageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho AdminUserCleanupService.
 * Kiểm tra các logic xóa cascade người dùng, buộc đăng xuất và dọn dẹp các phiên hoạt động.
 */
public class AdminUserCleanupServiceTest {

    private final UserDao userDao = UserDao.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();
    private final ImageService imageService = ImageService.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();

    private AdminUserCleanupService cleanupService;

    @BeforeAll
    public static void setUpClass() {
        SQLiteHelper.init();
    }

    @BeforeEach
    public void setUp() {
        cleanupService = new AdminUserCleanupService(
                userDao,
                auctionDao,
                itemDao,
                bidDao,
                transactionDao,
                imageDao,
                imageService,
                auctionService
        );
        cleanDb();
        RealtimeDatabase.clearAll();
    }

    @AfterEach
    public void tearDown() {
        cleanDb();
        RealtimeDatabase.clearAll();
    }

    private void cleanDb() {
        // Thứ tự dọn dẹp để giữ ràng buộc khóa ngoại (Bids -> Auctions -> Items -> Users)
        SQLiteHelper.update("DELETE FROM Bids");
        SQLiteHelper.update("DELETE FROM Transactions");
        SQLiteHelper.update("DELETE FROM Auctions");
        SQLiteHelper.update("DELETE FROM ItemImageLinks");
        SQLiteHelper.update("DELETE FROM Items");
        SQLiteHelper.update("DELETE FROM Users");
    }

    @Test
    public void testForceLogoutUser() {
        String username = "user_logout_test";
        User user = new User(username, "Nickname", "pass");
        userDao.create(user);

        // Thiết lập Mock ClientHandler
        TestClientHandler clientHandler = new TestClientHandler();
        clientHandler.setCurrentUsername(username);

        // Đăng ký active user
        RealtimeDatabase.addActiveUser(clientHandler, user);
        assertTrue(RealtimeDatabase.isUserOnline(username), "Người dùng phải online trước khi logout");

        // Gọi buộc đăng xuất
        cleanupService.forceLogoutUser(username, "Forced logout test message");

        // Kiểm tra
        assertFalse(RealtimeDatabase.isUserOnline(username), "Người dùng phải offline sau khi logout");
        assertNull(clientHandler.getCurrentUsername(), "Username trong ClientHandler phải bị reset về null");
        assertEquals(1, clientHandler.getSentEvents().size(), "Phải gửi đi 1 event");
        Event event = clientHandler.getSentEvents().get(0);
        assertEquals(EventType.FORCED_LOGOUT, event.getType(), "Loại event phải là FORCED_LOGOUT");
        assertEquals("Forced logout test message", event.getMessage(), "Message của event phải khớp");
    }

    @Test
    public void testDeleteUserCascade() {
        String sellerName = "user_seller_test";
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        String bidderName = "user_bidder_test";
        User bidder = new User(bidderName, "Bidder Nick", "pass");
        userDao.create(bidder);

        // 1. Tạo sản phẩm cho người bán
        Item item = new Item(sellerName, "Laptop Dell", "Mô tả máy dell", "Tech", "Laptop");
        itemDao.create(item);

        // 2. Tạo cuộc đấu giá cho sản phẩm của người bán
        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-cleanup-test",
            now,
            "Đấu giá Dell",
            "Mô tả",
            sellerName,
            item.getId(),
            null,
            1000.0,
            100.0,
            now.plusDays(1),
            now.plusDays(2),
            AuctionStatus.ACTIVE
        );
        auctionDao.create(auction);
        RealtimeDatabase.addRuntimeAuction(auction);

        // 3. Tạo Bid của người mua
        Bid bid = new Bid("bid-cleanup-test", now, auction.getId(), bidderName, 1200.0, false);
        bidDao.create(bid);
        auction.addBid(bid);

        // Thiết lập online session cho người bán để kiểm tra ngắt kết nối khi bị xóa
        TestClientHandler sellerClient = new TestClientHandler();
        sellerClient.setCurrentUsername(sellerName);
        RealtimeDatabase.addActiveUser(sellerClient, seller);
        assertTrue(RealtimeDatabase.isUserOnline(sellerName));

        // Gọi xóa người bán cascade
        cleanupService.deleteUser(seller);

        // 4. KIỂM TRA
        // Đảm bảo người bán đã bị xóa khỏi UserDao và offline
        assertNull(userDao.findByUsername(sellerName), "Tài khoản Seller phải bị xóa khỏi UserDao");
        assertFalse(RealtimeDatabase.isUserOnline(sellerName), "Seller phải bị xóa khỏi active users");
        assertTrue(sellerClient.isClosed(), "Kết nối của Seller phải bị đóng");
        assertEquals(1, sellerClient.getSentEvents().size());
        assertEquals(EventType.SERVER_NOTICE, sellerClient.getSentEvents().get(0).getType());

        // Đảm bảo đấu giá của người bán đã bị xóa cascade
        assertNull(auctionDao.findById(auction.getId()), "Đấu giá phải bị xóa khỏi database");
        assertNull(RealtimeDatabase.getRuntimeAuction(auction.getId()), "Đấu giá phải bị xóa khỏi runtime");

        // Đảm bảo sản phẩm của người bán bị xóa cascade
        assertNull(itemDao.findById(item.getId()), "Sản phẩm liên kết của người bán phải bị xóa cascade");

        // Đảm bảo lượt đặt giá liên quan đến cuộc đấu giá bị xóa cascade
        assertTrue(bidDao.findByAuctionId(auction.getId()).isEmpty(), "Lượt đặt giá của đấu giá đã xóa phải bị dọn dẹp");
    }

    /**
     * Lớp Mock ClientHandler kế thừa từ ClientHandler dùng riêng cho kiểm thử.
     */
    private static class TestClientHandler extends ClientHandler {
        private final List<Event> sentEvents = new ArrayList<>();
        private boolean closed = false;

        TestClientHandler() {
            super(null);
        }

        @Override
        public void sendEvent(Event event) {
            sentEvents.add(event);
        }

        @Override
        public void closeConnection() {
            closed = true;
        }

        @Override
        public boolean isInSession() {
            return getCurrentUsername() != null;
        }

        public List<Event> getSentEvents() {
            return sentEvents;
        }

        public boolean isClosed() {
            return closed;
        }
    }
}

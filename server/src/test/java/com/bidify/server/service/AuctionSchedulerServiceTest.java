package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.model.Event;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.contract.Observer;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) cho {@link AuctionSchedulerService}.
 * Sử dụng phản xạ Java (Reflection) để gọi trực tiếp phương thức đồng bộ private `syncRuntimeAuctions`.
 * Kiểm tra các tính năng:
 * - Chuyển trạng thái từ UPCOMING sang ACTIVE khi đến thời điểm bắt đầu.
 * - Giữ trạng thái UPCOMING nếu chưa đến thời điểm bắt đầu.
 * - Kết toán đấu giá (ACTIVE -> ENDED/Hết hạn) và xóa khỏi danh sách runtime khi đến thời điểm kết thúc.
 * - Giữ trạng thái ACTIVE nếu chưa đến thời điểm kết thúc.
 * - Kiểm tra khởi động và dừng Scheduler.
 * Có chú thích chi tiết bằng tiếng Việt.
 */
class AuctionSchedulerServiceTest {

    private final AuctionSchedulerService auctionSchedulerService = AuctionSchedulerService.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    private final List<String> createdUsernames = new ArrayList<>();
    private final List<String> createdItemIds = new ArrayList<>();
    private final List<String> createdAuctionIds = new ArrayList<>();

    // Danh sách lưu trữ các sự kiện được bắn ra kênh toàn cục (GlobalChannel)
    private final List<Event> globalEvents = new ArrayList<>();
    private final Observer globalObserver = globalEvents::add;

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
        globalEvents.clear();

        // Đăng ký observer để thu thập các event phát ra hệ thống
        RealtimeDatabase.getGlobalChannel().subscribe(globalObserver);
    }

    @AfterEach
    void tearDown() {
        // Hủy đăng ký để tránh rò rỉ bộ nhớ
        RealtimeDatabase.getGlobalChannel().unsubscribe(globalObserver);
        RealtimeDatabase.clearAll();

        // Xóa Bids và Transactions liên quan trước để giữ ràng buộc khóa ngoại (FK)
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
     * Ca kiểm thử: Đấu giá UPCOMING có thời gian bắt đầu ở quá khứ sẽ chuyển sang ACTIVE.
     * Xác thực: trạng thái trong SQLite và runtime đổi thành ACTIVE, đồng thời bắn event AUCTION_UPDATED.
     */
    @Test
    void syncRuntimeAuctionsTransitionsUpcomingToActive() throws Exception {
        // Tạo đấu giá UPCOMING có startTime ở quá khứ (10 phút trước)
        Auction auction = createTestAuction("Active-Upcoming", AuctionStatus.UPCOMING, 
                TimeUtil.nowInVietnam().minusMinutes(10), TimeUtil.nowInVietnam().plusHours(1));

        // Kích hoạt đồng bộ bằng Reflection
        triggerSync();

        // 1. Xác thực trạng thái được lưu trong SQLite đổi thành ACTIVE
        Auction updatedDb = auctionDao.findById(auction.getId());
        assertNotNull(updatedDb);
        assertEquals(AuctionStatus.ACTIVE, updatedDb.getStatus());

        // 2. Xác thực trạng thái trong Runtime Database đổi thành ACTIVE
        Auction updatedRuntime = RealtimeDatabase.getRuntimeAuction(auction.getId());
        assertNotNull(updatedRuntime);
        assertEquals(AuctionStatus.ACTIVE, updatedRuntime.getStatus());

        // 3. Xác thực có bắn sự kiện thông báo đấu giá đã active về client
        boolean hasActiveEvent = globalEvents.stream()
                .anyMatch(e -> e.getType() == EventType.AUCTION_UPDATED && e.getMessage().contains("active"));
        assertTrue(hasActiveEvent, "Hệ thống phải phát ra sự kiện AUCTION_UPDATED khi đấu giá chuyển sang hoạt động.");
    }

    /**
     * Ca kiểm thử: Đấu giá UPCOMING có thời gian bắt đầu ở tương lai phải giữ nguyên trạng thái.
     */
    @Test
    void syncRuntimeAuctionsDoesNotTransitionUpcomingInFuture() throws Exception {
        // Tạo đấu giá UPCOMING có startTime ở tương lai (10 phút sau)
        Auction auction = createTestAuction("Future-Upcoming", AuctionStatus.UPCOMING, 
                TimeUtil.nowInVietnam().plusMinutes(10), TimeUtil.nowInVietnam().plusHours(2));

        triggerSync();

        // Trạng thái giữ nguyên là UPCOMING
        Auction updatedDb = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.UPCOMING, updatedDb.getStatus());

        Auction updatedRuntime = RealtimeDatabase.getRuntimeAuction(auction.getId());
        assertEquals(AuctionStatus.UPCOMING, updatedRuntime.getStatus());

        // Không có event cập nhật nào bắn ra
        assertTrue(globalEvents.isEmpty(), "Không được phát sự kiện nếu trạng thái đấu giá không thay đổi.");
    }

    /**
     * Ca kiểm thử: Đấu giá ACTIVE có thời gian kết thúc ở quá khứ sẽ kết toán (Settle) và xóa khỏi runtime.
     * Xác thực: trạng thái đổi sang AWAITING_PAYMENT (vì không có ai bid nên về CANCELED hoặc AWAITING_PAYMENT tùy thuộc logic settle),
     * ở đây settleAuction sẽ đổi trạng thái đấu giá tùy theo người đặt giá, và xóa khỏi danh sách runtime.
     */
    @Test
    void syncRuntimeAuctionsTransitionsActiveToExpired() throws Exception {
        // Tạo đấu giá ACTIVE kết thúc từ 5 phút trước
        Auction auction = createTestAuction("Expired-Active", AuctionStatus.ACTIVE, 
                TimeUtil.nowInVietnam().minusHours(1), TimeUtil.nowInVietnam().minusMinutes(5));

        triggerSync();

        // 1. Xác thực đấu giá đã bị loại bỏ khỏi Runtime Database
        assertNull(RealtimeDatabase.getRuntimeAuction(auction.getId()), "Đấu giá đã hết hạn phải bị xóa khỏi runtime.");

        // 2. Xác thực trạng thái được lưu trong SQLite đã cập nhật (do không có bidder nên settleAuction chuyển về CANCELED)
        Auction updatedDb = auctionDao.findById(auction.getId());
        assertNotNull(updatedDb);
        assertEquals(AuctionStatus.CANCELED, updatedDb.getStatus());

        // 3. Xác thực có bắn sự kiện kết thúc đấu giá (AUCTION_ENDED)
        boolean hasEndedEvent = globalEvents.stream()
                .anyMatch(e -> e.getType() == EventType.AUCTION_ENDED && e.getMessage().contains("ended"));
        assertTrue(hasEndedEvent, "Hệ thống phải phát ra sự kiện AUCTION_ENDED khi đấu giá kết thúc.");
    }

    /**
     * Ca kiểm thử: Đấu giá ACTIVE chưa đến thời điểm kết thúc phải giữ nguyên trạng thái.
     */
    @Test
    void syncRuntimeAuctionsDoesNotTransitionActiveInFuture() throws Exception {
        // Tạo đấu giá ACTIVE còn 30 phút nữa mới kết thúc
        Auction auction = createTestAuction("Active-Running", AuctionStatus.ACTIVE, 
                TimeUtil.nowInVietnam().minusMinutes(30), TimeUtil.nowInVietnam().plusMinutes(30));

        triggerSync();

        // Trạng thái giữ nguyên là ACTIVE và vẫn nằm trong runtime
        Auction updatedDb = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.ACTIVE, updatedDb.getStatus());

        Auction updatedRuntime = RealtimeDatabase.getRuntimeAuction(auction.getId());
        assertNotNull(updatedRuntime);
        assertEquals(AuctionStatus.ACTIVE, updatedRuntime.getStatus());

        assertTrue(globalEvents.isEmpty(), "Không được phát sự kiện nếu đấu giá chưa hết hạn.");
    }

    /**
     * Ca kiểm thử: Khởi động và dừng hoạt động của scheduler.
     */
    @Test
    void testSchedulerStartAndStopLifecycle() {
        assertDoesNotThrow(() -> {
            auctionSchedulerService.start();
            // Đợi một khoảng thời gian cực ngắn để đảm bảo scheduler khởi chạy thành công
            Thread.sleep(50);
            auctionSchedulerService.stop();
        }, "Phương thức start và stop của AuctionSchedulerService phải hoạt động bình thường mà không gây ra ngoại lệ.");
    }

    // --- Hàm trợ giúp (Helper Methods) ---

    private void triggerSync() throws Exception {
        // Lấy phương thức private syncRuntimeAuctions qua Reflection
        Method method = AuctionSchedulerService.class.getDeclaredMethod("syncRuntimeAuctions");
        method.setAccessible(true);
        method.invoke(auctionSchedulerService);
    }

    private User createTestUser(String username) {
        User user = new User(username, username, PasswordUtil.hash("userPass"));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    private Auction createTestAuction(String name, AuctionStatus status, LocalDateTime start, LocalDateTime end) {
        String seller = uniqueUsername("seller");
        createTestUser(seller);

        Item item = new Item(seller, name, "Mô tả sản phẩm test", "CategoryTest", "TypeTest");
        itemDao.create(item);
        createdItemIds.add(item.getId());

        Auction auction = new Auction(
            seller, 
            item.getId(), 
            1000.0, 
            start,
            end
        );
        auction.setAuctionName(name);
        auction.setDescription("Mô tả đấu giá test");
        auction.setMinIncrement(100.0);
        auction.setStatus(status);
        auctionDao.create(auction);
        createdAuctionIds.add(auction.getId());

        // Thêm vào danh sách runtime để AuctionSchedulerService có thể quét qua
        RealtimeDatabase.addRuntimeAuction(auction);
        return auction;
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}

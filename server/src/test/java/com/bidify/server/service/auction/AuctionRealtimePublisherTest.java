package com.bidify.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

/**
 * Lớp kiểm thử (Unit Test) cho AuctionRealtimePublisher.
 * Bình luận mã nguồn bằng tiếng Việt.
 */
public class AuctionRealtimePublisherTest {

    private final UserDao userDao = UserDao.getInstance();
    private AuctionRealtimePublisher publisher;

    @BeforeAll
    public static void setUpClass() {
        SQLiteHelper.init();
    }

    @BeforeEach
    public void setUp() {
        publisher = new AuctionRealtimePublisher();
        cleanDb();
        RealtimeDatabase.clearAll();
    }

    @AfterEach
    public void tearDown() {
        cleanDb();
        RealtimeDatabase.clearAll();
    }

    private void cleanDb() {
        SQLiteHelper.update("DELETE FROM Users");
    }

    /**
     * Ca kiểm thử: Phát sự kiện cập nhật đấu giá tới kênh đấu giá cụ thể và kênh toàn cục.
     */
    @Test
    public void testPublishAuctionUpdate() {
        String username = "watcher_pub";
        User user = new User(username, "Watcher Pub", "pass");
        userDao.create(user);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-pub-update", now, "Rolex Watch", "Desc", "seller", "item-10",
            null, 1000.0, 100.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        // Giả lập client kết nối và đăng ký xem phiên đấu giá
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user); // Đăng ký active user (tự động subscribe global channel)
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), username); // Đăng ký xem đấu giá này

        AuctionDto dto = new AuctionDtoAssembler().toAuctionDto(auction, false);

        // Phát sự kiện cập nhật đấu giá
        publisher.publishAuctionUpdate(auction, dto, "Auction updated message");

        // Client phải nhận được 2 sự kiện:
        // 1 từ Global Channel (do addActiveUser)
        // 1 từ Auction Channel (do subscribeAuctionChannel)
        List<Event> received = client.getSentEvents();
        assertFalse(received.isEmpty());
        
        // Kiểm tra loại sự kiện
        boolean hasAuctionUpdatedEvent = received.stream()
                .anyMatch(e -> e.getType() == EventType.AUCTION_UPDATED && "Auction updated message".equals(e.getMessage()));
        assertTrue(hasAuctionUpdatedEvent, "Phải nhận được sự kiện AUCTION_UPDATED");
    }

    /**
     * Ca kiểm thử: Phát sự kiện đặt thầu thành công tới người theo dõi.
     */
    @Test
    public void testPublishAuctionBidEvent() {
        String username = "watcher_bid_pub";
        User user = new User(username, "Watcher Bid Pub", "pass");
        userDao.create(user);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-pub-bid", now, "Rolex Watch", "Desc", "seller", "item-11",
            null, 1000.0, 100.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), username);

        AuctionDto dto = new AuctionDtoAssembler().toAuctionDto(auction, false);

        // Phát sự kiện đặt thầu
        publisher.publishAuctionBidEvent(auction, dto, "New bid placed message");

        List<Event> received = client.getSentEvents();
        boolean hasBidPlacedEvent = received.stream()
                .anyMatch(e -> e.getType() == EventType.BID_PLACED && "New bid placed message".equals(e.getMessage()));
        assertTrue(hasBidPlacedEvent, "Phải nhận được sự kiện BID_PLACED");
    }

    /**
     * Ca kiểm thử: Phát sự kiện xóa cuộc đấu giá (Auction Deleted) lên kênh toàn cục.
     */
    @Test
    public void testPublishAuctionDeleted() {
        String username = "global_user";
        User user = new User(username, "Global User", "pass");
        userDao.create(user);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-deleted-id", now, "Deleted Item", "Desc", "seller", "item-12",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user); // Subscribe global channel

        publisher.publishAuctionDeleted(auction);

        List<Event> received = client.getSentEvents();
        boolean hasDeletedEvent = received.stream()
                .anyMatch(e -> e.getType() == EventType.AUCTION_DELETED && "auc-deleted-id".equals(e.getData()));
        assertTrue(hasDeletedEvent, "Phải nhận được sự kiện AUCTION_DELETED");
    }

    /**
     * Ca kiểm thử: Phát sự kiện thay đổi số dư ví (Balance Change) và thay đổi số tiền bị khóa (Locked Balance Change) tới client riêng lẻ.
     */
    @Test
    public void testPublishBalanceChanges() {
        String username = "wallet_user";
        User user = new User(username, "Wallet User", "pass");
        userDao.create(user);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // 1. Phát sự kiện đổi số dư khả dụng
        publisher.publishBalanceChange(username, 500.0);
        
        // 2. Phát sự kiện đổi số dư bị khóa
        publisher.publishLockedBalanceChange(username, -200.0);

        List<Event> received = client.getSentEvents();
        
        boolean hasWalletChanged = received.stream()
                .anyMatch(e -> e.getType() == EventType.WALLET_CHANGED && e.getMessage().contains("500.0"));
        boolean hasLockedBalanceChanged = received.stream()
                .anyMatch(e -> e.getType() == EventType.LOCKED_BALANCE_CHANGED && e.getMessage().contains("-200.0"));
        
        assertTrue(hasWalletChanged, "Phải nhận được sự kiện WALLET_CHANGED");
        assertTrue(hasLockedBalanceChanged, "Phải nhận được sự kiện LOCKED_BALANCE_CHANGED");
    }

    private static class TestClientHandler extends ClientHandler {
        private final List<Event> sentEvents = new ArrayList<>();

        TestClientHandler() {
            super(null);
        }

        @Override
        public void sendEvent(Event event) {
            sentEvents.add(event);
        }

        @Override
        public boolean isInSession() {
            return getCurrentUsername() != null;
        }

        public List<Event> getSentEvents() {
            return sentEvents;
        }
    }
}

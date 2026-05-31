package com.bidify.server.service.auction;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.model.Event;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.network.ClientHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho AuctionCascadeService.
 * Kiểm tra các tính năng giải phóng số dư bị giữ của bidder khi đấu giá bị xóa,
 * xóa cascade các thực thể liên quan và khôi phục trạng thái sản phẩm.
 */
public class AuctionCascadeServiceTest {

    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    private AuctionCascadeService cascadeService;

    @BeforeAll
    public static void setUpClass() {
        SQLiteHelper.init();
    }

    @BeforeEach
    public void setUp() {
        cascadeService = new AuctionCascadeService(
                auctionDao,
                itemDao,
                bidDao,
                transactionDao,
                userDao,
                new AuctionDtoAssembler(),
                new AuctionRealtimePublisher()
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
        SQLiteHelper.update("DELETE FROM Bids");
        SQLiteHelper.update("DELETE FROM Transactions");
        SQLiteHelper.update("DELETE FROM Auctions");
        SQLiteHelper.update("DELETE FROM ItemImageLinks");
        SQLiteHelper.update("DELETE FROM Items");
        SQLiteHelper.update("DELETE FROM Users");
    }

    @Test
    public void testReleaseCurrentLeaderLock() {
        String bidderUsername = "lock_bidder_test";
        User bidder = new User(bidderUsername, "Bidder Nick", "pass");
        
        // Cấu hình ví của bidder: tổng tiền 5000, đang giữ 2000
        Wallet wallet = bidder.getWallet();
        wallet.deposit(5000.0);
        wallet.lockBalance(2000.0);
        userDao.create(bidder);

        assertEquals(3000.0, bidder.getWallet().getAvailableBalance(), "Số dư khả dụng ban đầu phải là 3000");

        // Giả lập bidder online để nhận sự kiện locked balance changed
        TestClientHandler bidderClient = new TestClientHandler();
        bidderClient.setCurrentUsername(bidderUsername);
        RealtimeDatabase.addActiveUser(bidderClient, bidder);

        // Tạo cuộc đấu giá có bidder này dẫn đầu
        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-lock-test",
            now,
            "Đấu giá Test Lock",
            "Mô tả",
            "seller_usr",
            "item-123",
            bidderUsername,
            1000.0,
            100.0,
            now.plusDays(1),
            now.plusDays(2),
            AuctionStatus.ACTIVE
        );
        auction.setCurrentBid(2000.0);

        // Giải phóng lock của leader
        cascadeService.releaseCurrentLeaderLock(auction);

        // Nạp lại thông tin bidder để kiểm tra lưu trữ trong DB
        User updatedBidder = userDao.findByUsername(bidderUsername);
        assertNotNull(updatedBidder);
        assertEquals(0.0, updatedBidder.getWallet().getLockedBalance(), "Số dư bị khóa phải được trả về 0");
        assertEquals(5000.0, updatedBidder.getWallet().getAvailableBalance(), "Số dư khả dụng phải khôi phục thành 5000");

        // Kiểm tra sự kiện
        assertEquals(1, bidderClient.getSentEvents().size(), "Phải bắn 1 event");
        Event event = bidderClient.getSentEvents().get(0);
        assertEquals(EventType.LOCKED_BALANCE_CHANGED, event.getType());
        assertTrue(event.getMessage().contains("-2000.0"));
    }

    @Test
    public void testDeleteAuctionCascadeWithRestoreItem() {
        String sellerName = "seller_cascade";
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        String bidderName = "bidder_cascade";
        User bidder = new User(bidderName, "Bidder Nick", "pass");
        bidder.getWallet().deposit(2000.0);
        bidder.getWallet().lockBalance(1500.0);
        userDao.create(bidder);

        // 1. Tạo sản phẩm liên kết
        Item item = new Item(sellerName, "Đồng hồ cổ", "Mô tả đồng hồ", "Antiques", "Watch");
        item.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);
        itemDao.create(item);

        // 2. Tạo đấu giá liên kết sản phẩm
        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-cascade-test",
            now,
            "Đấu giá Đồng hồ cổ",
            "Mô tả",
            sellerName,
            item.getId(),
            bidderName,
            1000.0,
            100.0,
            now.plusDays(1),
            now.plusDays(2),
            AuctionStatus.ACTIVE
        );
        auction.setCurrentBid(1500.0);
        auctionDao.create(auction);
        RealtimeDatabase.addRuntimeAuction(auction);

        // 3. Tạo một số Bids
        Bid bid1 = new Bid("bid-cascade-1", now, auction.getId(), bidderName, 1100.0, false);
        Bid bid2 = new Bid("bid-cascade-2", now.plusSeconds(5), auction.getId(), bidderName, 1500.0, false);
        bidDao.create(bid1);
        bidDao.create(bid2);
        auction.addBid(bid1);
        auction.addBid(bid2);

        // Thực hiện xóa cascade và khôi phục sản phẩm về trạng thái AVAILABLE
        cascadeService.deleteAuctionCascade(auction, true);

        // 4. KIỂM TRA
        // Kiểm tra đấu giá bị xóa khỏi DB và runtime
        assertNull(auctionDao.findById(auction.getId()), "Đấu giá phải bị xóa khỏi database");
        assertNull(RealtimeDatabase.getRuntimeAuction(auction.getId()), "Đấu giá phải bị xóa khỏi runtime");

        // Kiểm tra số dư của leader được tự động giải phóng
        User updatedBidder = userDao.findByUsername(bidderName);
        assertEquals(0.0, updatedBidder.getWallet().getLockedBalance(), "Số dư bị khóa của bidder phải trả về 0");

        // Kiểm tra các bids bị dọn dẹp
        assertTrue(bidDao.findByAuctionId(auction.getId()).isEmpty(), "Toàn bộ bids liên quan phải bị xóa");

        // Kiểm tra sản phẩm được khôi phục về AVAILABLE
        Item updatedItem = itemDao.findById(item.getId());
        assertNotNull(updatedItem);
        assertEquals(ItemStatus.AVAILABLE, updatedItem.getAvailabilityStatus(), "Trạng thái sản phẩm phải khôi phục về AVAILABLE");
    }

    @Test
    public void testUpdateAuctionItemState() {
        String sellerName = "seller_state_test";
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        Item item = new Item(sellerName, "Ghế gỗ", "Mô tả ghế", "Furniture", "Chair");
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-state-test",
            now,
            "Đấu giá Ghế gỗ",
            "Mô tả",
            sellerName,
            item.getId(),
            null,
            500.0,
            50.0,
            now.plusDays(1),
            now.plusDays(2),
            AuctionStatus.ACTIVE
        );
        auctionDao.create(auction);

        // Cập nhật trạng thái sản phẩm đấu giá
        cascadeService.updateAuctionItemState(auction, "new_owner", ItemStatus.LOCKED_IN_AUCTION);

        // Kiểm tra
        Item updatedItem = itemDao.findById(item.getId());
        assertNotNull(updatedItem);
        assertEquals("new_owner", updatedItem.getOwnerUsername(), "Owner mới phải được cập nhật");
        assertEquals(ItemStatus.LOCKED_IN_AUCTION, updatedItem.getAvailabilityStatus(), "Trạng thái phải là LOCKED_IN_AUCTION");
    }

    /**
     * Lớp Mock ClientHandler dùng riêng cho kiểm thử.
     */
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

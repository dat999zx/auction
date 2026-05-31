package com.bidify.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.PlaceBidRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.SetAutoBidRequest;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

/**
 * Lớp kiểm thử (Unit Test) cho AuctionBidProcessor.
 * Các bình luận giải thích bằng tiếng Việt.
 */
public class AuctionBidProcessorTest {

    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    private AuctionBidProcessor bidProcessor;

    @BeforeAll
    public static void setUpClass() {
        SQLiteHelper.init();
    }

    @BeforeEach
    public void setUp() {
        bidProcessor = new AuctionBidProcessor(
                auctionDao,
                bidDao,
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
        SQLiteHelper.update("DELETE FROM Users");
        SQLiteHelper.update("DELETE FROM Auctions");
    }

    /**
     * Ca kiểm thử: Đấu giá tự động cạnh tranh (Competing AutoBids).
     * Bidder A cấu hình Max = 200 trước. Bidder B cấu hình Max = 150 sau.
     * Kết quả mong muốn: Bidder A dẫn đầu với giá thầu resolved = 160 (150 + minIncrement 10).
     */
    @Test
    public void testCompetingAutoBids() {
        String sellerName = "seller_bid";
        String bidderA = "bidder_a";
        String bidderB = "bidder_b";

        User seller = new User(sellerName, "Seller", "pass");
        User userA = new User(bidderA, "Bidder A", "pass");
        User userB = new User(bidderB, "Bidder B", "pass");

        userA.getWallet().deposit(1000.0);
        userB.getWallet().deposit(1000.0);

        userDao.create(seller);
        userDao.create(userA);
        userDao.create(userB);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-comp-autobid", now, "Item Name", "Desc", sellerName, "item-20",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        // Đưa các user lên online
        TestClientHandler clientA = new TestClientHandler();
        clientA.setCurrentUsername(bidderA);
        RealtimeDatabase.addActiveUser(clientA, userA);

        TestClientHandler clientB = new TestClientHandler();
        clientB.setCurrentUsername(bidderB);
        RealtimeDatabase.addActiveUser(clientB, userB);

        // 1. Cấu hình AutoBid cho A trước (Max 200)
        SetAutoBidRequest setReqA = new SetAutoBidRequest(auction.getId(), 200.0);
        Response resA = bidProcessor.setAutoBid(clientA, new Request(RequestType.SET_AUTO_BID, setReqA));
        assertEquals(RequestStatus.SUCCESS, resA.getStatus());

        // 2. Cấu hình AutoBid cho B sau (Max 150)
        SetAutoBidRequest setReqB = new SetAutoBidRequest(auction.getId(), 150.0);
        Response resB = bidProcessor.setAutoBid(clientB, new Request(RequestType.SET_AUTO_BID, setReqB));
        assertEquals(RequestStatus.SUCCESS, resB.getStatus());

        // Xác thực kết quả: A là người dẫn đầu tại giá 160
        assertEquals(bidderA, auction.getCurrentBidderUsername());
        assertEquals(160.0, auction.getCurrentBid(), 0.001);
    }

    /**
     * Ca kiểm thử: Độ ưu tiên thời gian khi trùng ngân sách AutoBid (Priority ties).
     * Bidder A và Bidder B cùng cấu hình Max = 200, nhưng A cấu hình trước.
     * Kết quả mong muốn: Bidder A dẫn đầu tại giá resolved = 200.
     */
    @Test
    public void testPriorityTiesForSameAutoBidMax() {
        String sellerName = "seller_bid_2";
        String bidderA = "bidder_a_2";
        String bidderB = "bidder_b_2";

        User seller = new User(sellerName, "Seller", "pass");
        User userA = new User(bidderA, "Bidder A", "pass");
        User userB = new User(bidderB, "Bidder B", "pass");

        userA.getWallet().deposit(1000.0);
        userB.getWallet().deposit(1000.0);

        userDao.create(seller);
        userDao.create(userA);
        userDao.create(userB);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-priority-tie", now, "Item Name", "Desc", sellerName, "item-21",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        TestClientHandler clientA = new TestClientHandler();
        clientA.setCurrentUsername(bidderA);
        RealtimeDatabase.addActiveUser(clientA, userA);

        TestClientHandler clientB = new TestClientHandler();
        clientB.setCurrentUsername(bidderB);
        RealtimeDatabase.addActiveUser(clientB, userB);

        // A thiết lập Max 200
        SetAutoBidRequest setReqA = new SetAutoBidRequest(auction.getId(), 200.0);
        bidProcessor.setAutoBid(clientA, new Request(RequestType.SET_AUTO_BID, setReqA));

        // B thiết lập Max 200
        SetAutoBidRequest setReqB = new SetAutoBidRequest(auction.getId(), 200.0);
        bidProcessor.setAutoBid(clientB, new Request(RequestType.SET_AUTO_BID, setReqB));

        // Kết quả: A thắng do có độ ưu tiên thời gian tốt hơn, resolved lên đúng Max 200
        assertEquals(bidderA, auction.getCurrentBidderUsername());
        assertEquals(200.0, auction.getCurrentBid(), 0.001);
    }

    /**
     * Ca kiểm thử: Khóa số dư ví khi đặt giá thầu (Wallet locks).
     * Đặt thầu 150 -> số dư bị khóa tăng 150.
     * Đặt thầu mới cao hơn từ người khác -> giải khóa người cũ, khóa người mới.
     */
    @Test
    public void testWalletLocksOnBidding() {
        String sellerName = "seller_bid_3";
        String bidderA = "bidder_a_3";
        String bidderB = "bidder_b_3";

        User seller = new User(sellerName, "Seller", "pass");
        User userA = new User(bidderA, "Bidder A", "pass");
        User userB = new User(bidderB, "Bidder B", "pass");

        userA.getWallet().deposit(1000.0);
        userB.getWallet().deposit(1000.0);

        userDao.create(seller);
        userDao.create(userA);
        userDao.create(userB);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-wallet-lock", now, "Item Name", "Desc", sellerName, "item-22",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        TestClientHandler clientA = new TestClientHandler();
        clientA.setCurrentUsername(bidderA);
        RealtimeDatabase.addActiveUser(clientA, userA);

        TestClientHandler clientB = new TestClientHandler();
        clientB.setCurrentUsername(bidderB);
        RealtimeDatabase.addActiveUser(clientB, userB);

        // 1. A đặt thầu 150
        PlaceBidRequest bidReqA = new PlaceBidRequest(auction.getId(), 150.0);
        Response resA = bidProcessor.placeBid(clientA, new Request(RequestType.PLACE_BID, bidReqA));
        assertEquals(RequestStatus.SUCCESS, resA.getStatus());

        // Ví A phải bị khóa 150
        assertEquals(150.0, userA.getWallet().getLockedBalance(), 0.001);
        assertEquals(850.0, userA.getWallet().getAvailableBalance(), 0.001);

        // 2. B đặt thầu cao hơn 200
        PlaceBidRequest bidReqB = new PlaceBidRequest(auction.getId(), 200.0);
        Response resB = bidProcessor.placeBid(clientB, new Request(RequestType.PLACE_BID, bidReqB));
        assertEquals(RequestStatus.SUCCESS, resB.getStatus());

        // Ví A phải được trả lại 0 khóa, ví B bị khóa 200
        assertEquals(0.0, userA.getWallet().getLockedBalance(), 0.001);
        assertEquals(200.0, userB.getWallet().getLockedBalance(), 0.001);
    }

    /**
     * Ca kiểm thử: Khôi phục trạng thái khi ghi nhận dữ liệu bị lỗi (Rollback on DAO failure).
     * Bằng cách đặt ID của auction thành null để gây lỗi NOT NULL constraint trong SQLite khi chèn Bid.
     */
    @Test
    public void testRollbackOnDaoFailure() {
        String sellerName = "seller_bid_4";
        String bidderName = "bidder_rollback";

        User seller = new User(sellerName, "Seller", "pass");
        User user = new User(bidderName, "Bidder", "pass");
        user.getWallet().deposit(1000.0);

        userDao.create(seller);
        userDao.create(user);

        LocalDateTime now = TimeUtil.nowInVietnam();
        String auctionId = "auc-rollback-test";
        Auction auction = new Auction(
            auctionId, now, "Item Name", "Desc", sellerName, "item-23",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(bidderName);
        RealtimeDatabase.addActiveUser(client, user);

        // Cố tình sửa ID đấu giá thành null để kích hoạt lỗi DB khi chèn Bid (Bids.auctionId NOT NULL)
        auction.setId(null);

        try {
            PlaceBidRequest bidReq = new PlaceBidRequest(auctionId, 150.0);
            Response response = bidProcessor.placeBid(client, new Request(RequestType.PLACE_BID, bidReq));
            
            // Phải trả về Response lỗi FAILED do chèn SQL lỗi
            assertEquals(RequestStatus.FAILED, response.getStatus());

            // Xác thực Rollback:
            // 1. Số dư bị khóa của user phải khôi phục về 0
            assertEquals(0.0, user.getWallet().getLockedBalance(), 0.001);
            assertEquals(1000.0, user.getWallet().getAvailableBalance(), 0.001);

            // 2. Trạng thái phiên đấu giá khôi phục (chưa có bidder dẫn đầu, bid lượng = 0)
            assertNull(auction.getCurrentBidderUsername());
            assertEquals(0.0, auction.getCurrentBid(), 0.001);
            assertEquals(0, auction.getBids().size());
        } finally {
            // Khôi phục ID để teardown không lỗi
            auction.setId(auctionId);
        }
    }

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

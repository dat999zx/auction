package com.bidify.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.AuctionResolutionAction;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.model.ConfirmDeliveryRequest;
import com.bidify.common.model.Event;
import com.bidify.common.model.PayAuctionRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.ResolveAuctionRequest;
import com.bidify.common.model.Response;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Admin;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Item;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

/**
 * Lớp kiểm thử (Unit Test) cho AuctionSettlementProcessor.
 * Đảm bảo các bình luận sử dụng tiếng Việt.
 */
public class AuctionSettlementProcessorTest {

    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    private AuctionSettlementProcessor processor;

    @BeforeAll
    public static void setUpClass() {
        SQLiteHelper.init();
    }

    @BeforeEach
    public void setUp() {
        processor = new AuctionSettlementProcessor(
                auctionDao,
                transactionDao,
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

    /**
     * Ca kiểm thử: Người thắng đấu giá thực hiện thanh toán thành công.
     */
    @Test
    public void testPayAuctionSuccessfully() {
        String sellerName = "seller_pay";
        String winnerName = "winner_pay";
        
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        User winner = new User(winnerName, "Winner Nick", "pass");
        winner.getWallet().deposit(3000.0);
        winner.getWallet().lockBalance(2000.0); // Khóa tiền bid
        userDao.create(winner);

        Item item = new Item(sellerName, "Laptop", "Desc", "Tech", "Dell");
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-pay-ok", now, "Laptop Auction", "Desc", sellerName, item.getId(),
            winnerName, 1000.0, 100.0, now.plusDays(1), now.plusDays(2), AuctionStatus.AWAITING_PAYMENT
        );
        auction.setCurrentBid(2000.0);
        auctionDao.create(auction);

        // Giả lập client session cho winner
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(winnerName);
        RealtimeDatabase.addActiveUser(client, winner);

        // Gửi yêu cầu thanh toán
        PayAuctionRequest payReq = new PayAuctionRequest(auction.getId());
        Request request = new Request(RequestType.PAY_AUCTION, payReq);

        Response response = processor.payAuction(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Paid for auction successfully", response.getMessage());

        // Kiểm tra số dư ví winner sau khi thanh toán
        User updatedWinner = userDao.findByUsername(winnerName);
        assertNotNull(updatedWinner);
        assertEquals(1000.0, updatedWinner.getWallet().getBalance(), 0.001, "Tổng số dư ví còn lại là 1000");
        assertEquals(0.0, updatedWinner.getWallet().getLockedBalance(), 0.001, "Locked balance phải về 0");

        // Kiểm tra trạng thái đấu giá trong DB
        Auction updatedAuction = auctionDao.findById(auction.getId());
        assertNotNull(updatedAuction);
        assertEquals(AuctionStatus.AWAITING_DELIVERY, updatedAuction.getStatus(), "Trạng thái đấu giá phải là AWAITING_DELIVERY");

        // Kiểm tra giao dịch thanh toán được lưu
        List<Transaction> txs = transactionDao.findByUsername(winnerName);
        assertFalse(txs.isEmpty());
        Transaction payTx = txs.stream().filter(t -> t.getType() == TransactionType.AUCTION_PAY).findFirst().orElse(null);
        assertNotNull(payTx);
        assertEquals(2000.0, payTx.getAmount(), 0.001);
        assertEquals(auction.getId(), payTx.getAuctionId());
    }

    /**
     * Ca kiểm thử: Thanh toán thất bại do người gọi không phải người thắng.
     */
    @Test
    public void testPayAuctionFailsWhenNotWinner() {
        String sellerName = "seller_pay_fail";
        String winnerName = "winner_pay_fail";
        String otherName = "other_user";
        
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        User winner = new User(winnerName, "Winner Nick", "pass");
        winner.getWallet().deposit(3000.0);
        winner.getWallet().lockBalance(2000.0);
        userDao.create(winner);

        User other = new User(otherName, "Other Nick", "pass");
        userDao.create(other);

        Item item = new Item(sellerName, "Laptop", "Desc", "Tech", "Dell");
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-pay-fail-winner", now, "Laptop Auction", "Desc", sellerName, item.getId(),
            winnerName, 1000.0, 100.0, now.plusDays(1), now.plusDays(2), AuctionStatus.AWAITING_PAYMENT
        );
        auction.setCurrentBid(2000.0);
        auctionDao.create(auction);

        // Giả lập client session cho người thứ ba
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(otherName);
        RealtimeDatabase.addActiveUser(client, other);

        PayAuctionRequest payReq = new PayAuctionRequest(auction.getId());
        Request request = new Request(RequestType.PAY_AUCTION, payReq);

        Response response = processor.payAuction(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertTrue(response.getMessage().contains("winning bidder"), "Thông báo lỗi phải đề cập việc chỉ bidder chiến thắng mới được thanh toán");
    }

    /**
     * Ca kiểm thử: Người bán xác nhận đã giao hàng thành công.
     */
    @Test
    public void testConfirmAuctionDeliverySuccessfully() {
        String sellerName = "seller_deliv";
        String winnerName = "winner_deliv";
        
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        User winner = new User(winnerName, "Winner Nick", "pass");
        userDao.create(winner);

        Item item = new Item(sellerName, "Laptop", "Desc", "Tech", "Dell");
        item.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-deliv-ok", now, "Laptop Auction", "Desc", sellerName, item.getId(),
            winnerName, 1000.0, 100.0, now.minusDays(2), now.minusDays(1), AuctionStatus.AWAITING_DELIVERY
        );
        auction.setCurrentBid(2000.0);
        auctionDao.create(auction);
        RealtimeDatabase.addRuntimeAuction(auction);

        // Giả lập seller thực hiện xác nhận
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(sellerName);
        RealtimeDatabase.addActiveUser(client, seller);

        ConfirmDeliveryRequest delivReq = new ConfirmDeliveryRequest(auction.getId());
        Request request = new Request(RequestType.CONFIRM_AUCTION_DELIVERY, delivReq);

        Response response = processor.confirmAuctionDelivery(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        
        // Kiểm tra tiền được cộng vào ví seller
        User updatedSeller = userDao.findByUsername(sellerName);
        assertEquals(2000.0, updatedSeller.getWallet().getBalance(), 0.001);

        // Kiểm tra trạng thái đấu giá thành COMPLETED
        Auction updatedAuction = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.COMPLETED, updatedAuction.getStatus());

        // Kiểm tra sản phẩm đổi chủ sang winner và trạng thái là AVAILABLE
        Item updatedItem = itemDao.findById(item.getId());
        assertEquals(winnerName, updatedItem.getOwnerUsername());
        assertEquals(ItemStatus.AVAILABLE, updatedItem.getAvailabilityStatus());

        // Kiểm tra giao dịch AUCTION_PROFIT cho seller
        List<Transaction> txs = transactionDao.findByUsername(sellerName);
        assertFalse(txs.isEmpty());
        Transaction profitTx = txs.stream().filter(t -> t.getType() == TransactionType.AUCTION_PROFIT).findFirst().orElse(null);
        assertNotNull(profitTx);
        assertEquals(2000.0, profitTx.getAmount(), 0.001);
    }

    /**
     * Ca kiểm thử: Người dùng thường không thể xác nhận giao hàng nếu không phải người bán.
     */
    @Test
    public void testConfirmAuctionDeliveryFailsWhenNotSeller() {
        String sellerName = "seller_deliv_fail";
        String winnerName = "winner_deliv_fail";
        
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        User winner = new User(winnerName, "Winner Nick", "pass");
        userDao.create(winner);

        Item item = new Item(sellerName, "Laptop", "Desc", "Tech", "Dell");
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-deliv-fail-role", now, "Laptop Auction", "Desc", sellerName, item.getId(),
            winnerName, 1000.0, 100.0, now.minusDays(2), now.minusDays(1), AuctionStatus.AWAITING_DELIVERY
        );
        auction.setCurrentBid(2000.0);
        auctionDao.create(auction);

        // Giả lập winner cố gắng tự xác nhận giao hàng
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(winnerName);
        RealtimeDatabase.addActiveUser(client, winner);

        ConfirmDeliveryRequest delivReq = new ConfirmDeliveryRequest(auction.getId());
        Request request = new Request(RequestType.CONFIRM_AUCTION_DELIVERY, delivReq);

        Response response = processor.confirmAuctionDelivery(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertTrue(response.getMessage().contains("Only the seller or an admin"), "Thông báo lỗi phải đề cập việc chỉ seller hoặc admin mới được xác nhận");
    }

    /**
     * Ca kiểm thử: Admin giải quyết cưỡng chế hoàn thành cuộc đấu giá.
     */
    @Test
    public void testResolveAuctionCompleteByAdmin() {
        String sellerName = "seller_resolve_c";
        String winnerName = "winner_resolve_c";
        
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        User winner = new User(winnerName, "Winner Nick", "pass");
        userDao.create(winner);

        Admin admin = new Admin("admin_res", "Admin Nick", "pass");
        userDao.create(admin);

        Item item = new Item(sellerName, "Laptop", "Desc", "Tech", "Dell");
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-res-complete", now, "Laptop Auction", "Desc", sellerName, item.getId(),
            winnerName, 1000.0, 100.0, now.minusDays(2), now.minusDays(1), AuctionStatus.AWAITING_DELIVERY
        );
        auction.setCurrentBid(2000.0);
        auctionDao.create(auction);
        RealtimeDatabase.addRuntimeAuction(auction);

        // Giả lập admin gửi yêu cầu resolve COMPLETE
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(admin.getUsername());
        RealtimeDatabase.addActiveUser(client, admin);

        ResolveAuctionRequest resolveReq = new ResolveAuctionRequest(auction.getId(), AuctionResolutionAction.COMPLETE);
        Request request = new Request(RequestType.RESOLVE_AUCTION, resolveReq);

        Response response = processor.resolveAuction(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Auction resolved successfully", response.getMessage());

        // Kiểm tra đấu giá hoàn tất thành công
        Auction updatedAuction = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.COMPLETED, updatedAuction.getStatus());
    }

    /**
     * Ca kiểm thử: Admin hủy cuộc đấu giá đang ở trạng thái chờ thanh toán (AWAITING_PAYMENT).
     * Đảm bảo giải khóa số dư của bidder dẫn đầu.
     */
    @Test
    public void testResolveAuctionCancelAwaitingPaymentByAdmin() {
        String sellerName = "seller_resolve_ap";
        String winnerName = "winner_resolve_ap";
        
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        User winner = new User(winnerName, "Winner Nick", "pass");
        winner.getWallet().deposit(3000.0);
        winner.getWallet().lockBalance(2000.0); // Tiền bị khóa
        userDao.create(winner);

        Admin admin = new Admin("admin_res_ap", "Admin Nick", "pass");
        userDao.create(admin);

        Item item = new Item(sellerName, "Laptop", "Desc", "Tech", "Dell");
        item.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-res-cancel-ap", now, "Laptop", "Desc", sellerName, item.getId(),
            winnerName, 1000.0, 100.0, now.minusDays(1), now.plusDays(1), AuctionStatus.AWAITING_PAYMENT
        );
        auction.setCurrentBid(2000.0);
        auctionDao.create(auction);
        RealtimeDatabase.addRuntimeAuction(auction);

        // Giả lập admin
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(admin.getUsername());
        RealtimeDatabase.addActiveUser(client, admin);

        // Yêu cầu hủy bỏ
        ResolveAuctionRequest resolveReq = new ResolveAuctionRequest(auction.getId(), AuctionResolutionAction.CANCEL);
        Request request = new Request(RequestType.RESOLVE_AUCTION, resolveReq);

        Response response = processor.resolveAuction(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        // Số dư bị khóa của bidder phải trả về 0, tổng tiền vẫn là 3000
        User updatedWinner = userDao.findByUsername(winnerName);
        assertEquals(0.0, updatedWinner.getWallet().getLockedBalance(), 0.001);
        assertEquals(3000.0, updatedWinner.getWallet().getBalance(), 0.001);

        // Trạng thái đấu giá thành CANCELED
        Auction updatedAuction = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.CANCELED, updatedAuction.getStatus());

        // Sản phẩm trả lại trạng thái AVAILABLE
        Item updatedItem = itemDao.findById(item.getId());
        assertEquals(ItemStatus.AVAILABLE, updatedItem.getAvailabilityStatus());
    }

    /**
     * Ca kiểm thử: Admin hủy cuộc đấu giá đang chờ giao hàng (AWAITING_DELIVERY).
     * Đảm bảo hoàn trả tiền đã thanh toán lại cho bidder.
     */
    @Test
    public void testResolveAuctionCancelAwaitingDeliveryByAdmin() {
        String sellerName = "seller_resolve_ad";
        String winnerName = "winner_resolve_ad";
        
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        User winner = new User(winnerName, "Winner Nick", "pass");
        winner.getWallet().deposit(1000.0); // Ví ban đầu sau khi đã thanh toán (giả sử trước đó có 3000)
        userDao.create(winner);

        Admin admin = new Admin("admin_res_ad", "Admin Nick", "pass");
        userDao.create(admin);

        Item item = new Item(sellerName, "Laptop", "Desc", "Tech", "Dell");
        item.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-res-cancel-ad", now, "Laptop", "Desc", sellerName, item.getId(),
            winnerName, 1000.0, 100.0, now.minusDays(2), now.minusDays(1), AuctionStatus.AWAITING_DELIVERY
        );
        auction.setCurrentBid(2000.0);
        auctionDao.create(auction);
        RealtimeDatabase.addRuntimeAuction(auction);

        // Giả lập admin
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(admin.getUsername());
        RealtimeDatabase.addActiveUser(client, admin);

        // Yêu cầu hủy bỏ
        ResolveAuctionRequest resolveReq = new ResolveAuctionRequest(auction.getId(), AuctionResolutionAction.CANCEL);
        Request request = new Request(RequestType.RESOLVE_AUCTION, resolveReq);

        Response response = processor.resolveAuction(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        // Bidder phải nhận lại 2000 hoàn trả, số dư ví trở thành 3000
        User updatedWinner = userDao.findByUsername(winnerName);
        assertEquals(3000.0, updatedWinner.getWallet().getBalance(), 0.001);

        // Có giao dịch AUCTION_REFUND
        List<Transaction> txs = transactionDao.findByUsername(winnerName);
        Transaction refundTx = txs.stream().filter(t -> t.getType() == TransactionType.AUCTION_REFUND).findFirst().orElse(null);
        assertNotNull(refundTx);
        assertEquals(2000.0, refundTx.getAmount(), 0.001);

        // Trạng thái đấu giá thành CANCELED
        Auction updatedAuction = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.CANCELED, updatedAuction.getStatus());
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

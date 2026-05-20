package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.AuctionResolutionAction;
import com.bidify.common.model.DisableAutoBidRequest;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.PlaceBidRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.SetAutoBidRequest;
import com.bidify.common.model.PayAuctionRequest;
import com.bidify.common.model.ConfirmDeliveryRequest;
import com.bidify.common.model.ResolveAuctionRequest;
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
import com.bidify.server.utility.ServiceUtil;

class AuctionServiceTest {
    // Giả định AuctionService áp dụng Singleton pattern tương tự AuthService
    private final AuctionService auctionService = AuctionService.getInstance();
    
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    
    // Lưu lại IDs do test tạo ra để dọn sạch sau mỗi test
    private final List<String> createdAuctionIds = new ArrayList<>();
    private final List<String> createdItemIds = new ArrayList<>();
    private final List<String> createdUsernames = new ArrayList<>();

    // dùng để khởi tạo cơ sở dữ liệu
    @BeforeAll
    static void initDatabase() {
        SQLiteHelper.init();
    }

    // dùng để thiết lập up
    @BeforeEach
    void setUp() {
        RealtimeDatabase.clearAll();
        createdAuctionIds.clear();
        createdItemIds.clear();
        createdUsernames.clear();
    }

    // dùng để tear down
    @AfterEach
    void tearDown() {
        RealtimeDatabase.clearAll();
        
        // Dọn dẹp auction và bids để tránh vi phạm khóa ngoại và làm rác DB thật
        for (String auctionId : createdAuctionIds) {
            List<Bid> bids = bidDao.findByAuctionId(auctionId);
            for (Bid bid : bids) {
                bidDao.deleteById(bid.getId());
            }
            auctionDao.deleteById(auctionId);
        }
        
        for (String itemId : createdItemIds) {
            itemDao.deleteById(itemId);
        }
        
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }
    }

    // dùng để lấy live danh sách đấu giá successfully returns list
    @Test
    void getLiveAuctionsSuccessfullyReturnsList() {
        // Seed dữ liệu
        // dùng để tạo test đấu giá
        createTestAuction("Test Active", AuctionStatus.ACTIVE);
        
        // Gọi service xử lý
        Response response = auctionService.getAllLiveAuctions(); 
        
        // Xác nhận trạng thái Response là SUCCESS và chứa dữ liệu
        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getData());
        List<?> data = (List<?>) response.getData();
        assertFalse(data.isEmpty());
    }

    // dùng để place lượt đặt giá successfully updates đấu giá and creates lượt đặt giá
    @Test
    void placeBidSuccessfullyUpdatesAuctionAndCreatesBid() {
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createTestUser(bidderUsername, "secret123");
        bidder.getWallet().deposit(5000.0);
        userDao.save(bidder, false);

        Auction auction = createTestAuction("Bid Auction", AuctionStatus.ACTIVE);
        
        // Tạo client mô phỏng với trạng thái đã đăng nhập
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(bidderUsername);
        RealtimeDatabase.addActiveUser(client, bidder);

        Request request = new Request(RequestType.PLACE_BID, new PlaceBidRequest(auction.getId(), auction.getStartingPrice() + auction.getMinIncrement() + 100));
        Response response = auctionService.placeBid(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        
        // Kiểm tra xem Auction có được cập nhật giá mới trong DB không
        Auction updatedAuction = auctionDao.findById(auction.getId());
        assertEquals(bidderUsername, updatedAuction.getCurrentBidderUsername());
        
        // Kiểm tra lịch sử Bid có ghi xuống DB chính xác chưa
        List<Bid> bids = bidDao.findByAuctionId(auction.getId());
        assertFalse(bids.isEmpty());
        assertEquals(bidderUsername, bids.get(bids.size() - 1).getBidderUsername());
    }

    // dùng để place lượt đặt giá fails when client not logged trong
    @Test
    void placeBidFailsWhenClientNotLoggedIn() {
        Auction auction = createTestAuction("Bid Auction", AuctionStatus.ACTIVE);
        
        // Client không gắn User => Không nằm trong session
        TestClientHandler unauthClient = new TestClientHandler();

        Request request = new Request(RequestType.PLACE_BID, new PlaceBidRequest(auction.getId(), 2000.0));
        Response response = auctionService.placeBid(unauthClient, request);

        // Yêu cầu đấu giá phải bị từ chối
        assertEquals(RequestStatus.FAILED, response.getStatus());
    }

    // dùng để place lượt đặt giá fails when đấu giá kiểm tra xem not active
    @Test
    void placeBidFailsWhenAuctionIsNotActive() {
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createTestUser(bidderUsername, "secret123");
        
        // Đấu giá đã đóng hoặc kết thúc
        Auction endedAuction = createTestAuction("Ended Auction", AuctionStatus.ENDED);
        
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(bidderUsername);
        RealtimeDatabase.addActiveUser(client, bidder);

        Request request = new Request(RequestType.PLACE_BID, new PlaceBidRequest(endedAuction.getId(), 2000.0));
        Response response = auctionService.placeBid(client, request);

        // Yêu cầu đấu giá thất bại vì AuctionStatus.ENDED
        assertEquals(RequestStatus.FAILED, response.getStatus());
    }

    // dùng để thiết lập auto lượt đặt giá can immediately overtake tại minimum needed số tiền
    @Test
    void setAutoBidCanImmediatelyOvertakeAtMinimumNeededAmount() {
        String bidder1 = uniqueUsername("autoA");
        String bidder2 = uniqueUsername("autoB");
        User first = createFundedActiveUser(bidder1, 5000.0);
        User second = createFundedActiveUser(bidder2, 5000.0);
        Auction auction = createTestAuction("Proxy Auction", AuctionStatus.ACTIVE);

        TestClientHandler firstClient = sessionClient(first);
        TestClientHandler secondClient = sessionClient(second);

        Response firstBid = auctionService.placeBid(firstClient, new Request(
            RequestType.PLACE_BID,
            new PlaceBidRequest(auction.getId(), 1200.0)
        ));
        assertEquals(RequestStatus.SUCCESS, firstBid.getStatus());

        Response response = auctionService.setAutoBid(secondClient, new Request(
            RequestType.SET_AUTO_BID,
            new SetAutoBidRequest(auction.getId(), 1500.0)
        ));

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        Auction updated = auctionDao.findById(auction.getId());
        assertEquals(bidder2, updated.getCurrentBidderUsername());
        assertEquals(1300.0, updated.getCurrentBid());
    }

    // dùng để raising leader auto lượt đặt giá max does not tạo extra lịch sử entry when visible lượt đặt giá stays same
    @Test
    void raisingLeaderAutoBidMaxDoesNotCreateExtraHistoryEntryWhenVisibleBidStaysSame() {
        String bidder = uniqueUsername("leader");
        User leader = createFundedActiveUser(bidder, 5000.0);
        Auction auction = createTestAuction("Leader Proxy Auction", AuctionStatus.ACTIVE);
        TestClientHandler client = sessionClient(leader);

        Response firstBid = auctionService.placeBid(client, new Request(
            RequestType.PLACE_BID,
            new PlaceBidRequest(auction.getId(), 1200.0)
        ));
        assertEquals(RequestStatus.SUCCESS, firstBid.getStatus());

        int before = bidDao.findByAuctionId(auction.getId()).size();
        Response response = auctionService.setAutoBid(client, new Request(
            RequestType.SET_AUTO_BID,
            new SetAutoBidRequest(auction.getId(), 3000.0)
        ));

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals(before, bidDao.findByAuctionId(auction.getId()).size());
    }

    // dùng để disable auto lượt đặt giá succeeds cho leading bidder
    @Test
    void disableAutoBidSucceedsForLeadingBidder() {
        String bidder = uniqueUsername("autoDisable");
        User leader = createFundedActiveUser(bidder, 5000.0);
        Auction auction = createTestAuction("Disable Proxy Auction", AuctionStatus.ACTIVE);
        TestClientHandler client = sessionClient(leader);

        Response firstBid = auctionService.placeBid(client, new Request(
            RequestType.PLACE_BID,
            new PlaceBidRequest(auction.getId(), 1200.0)
        ));
        assertEquals(RequestStatus.SUCCESS, firstBid.getStatus());

        Response setProxy = auctionService.setAutoBid(client, new Request(
            RequestType.SET_AUTO_BID,
            new SetAutoBidRequest(auction.getId(), 2000.0)
        ));
        assertEquals(RequestStatus.SUCCESS, setProxy.getStatus());

        Response disableProxy = auctionService.disableAutoBid(client, new Request(
            RequestType.DISABLE_AUTO_BID,
            new DisableAutoBidRequest(auction.getId())
        ));

        assertEquals(RequestStatus.SUCCESS, disableProxy.getStatus());
        Auction updated = auctionDao.findById(auction.getId());
        assertEquals(bidder, updated.getCurrentBidderUsername());
        assertEquals(1200.0, updated.getCurrentBid());
    }

    // dùng để lấy đấu giá chi tiết marks current danh sách người dùng active auto lượt đặt giá
    @Test
    void getAuctionDetailMarksCurrentUsersActiveAutoBid() {
        String bidder = uniqueUsername("autoDetail");
        User user = createFundedActiveUser(bidder, 5000.0);
        Auction auction = createTestAuction("Auto Detail Auction", AuctionStatus.ACTIVE);
        TestClientHandler client = sessionClient(user);

        Response setAutoBid = auctionService.setAutoBid(client, new Request(
            RequestType.SET_AUTO_BID,
            new SetAutoBidRequest(auction.getId(), 2000.0)
        ));
        assertEquals(RequestStatus.SUCCESS, setAutoBid.getStatus());

        Response detail = auctionService.getDetail(client, new Request(
            RequestType.GET_AUCTION_DETAIL,
            new GetAuctionDetailRequest(auction.getId())
        ));

        assertEquals(RequestStatus.SUCCESS, detail.getStatus());
        AuctionDto dto = (AuctionDto) detail.getData();
        // dùng để assert not null
        assertNotNull(dto);
        assertTrue(dto.isCurrentUserAutoBidActive());
        assertEquals(2000.0, dto.getCurrentUserAutoBidMax());
    }

    // --- Helper Methods ---

    // dùng để tạo test người dùng
    private User createTestUser(String username, String rawPassword) {
        User user = new User(username, username, PasswordUtil.hash(rawPassword));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    // dùng để tạo funded active người dùng
    private User createFundedActiveUser(String username, double balance) {
        User user = createTestUser(username, "secret123");
        user.getWallet().deposit(balance);
        userDao.save(user, false);
        return user;
    }

    // dùng để tạo test đấu giá
    private Auction createTestAuction(String name, AuctionStatus status) {
        // Bắt buộc tạo một mock Seller trước khi tạo Auction để giữ toàn vẹn khóa ngoại (FK)
        String sellerUsername = uniqueUsername("seller");
        // dùng để tạo test người dùng
        createTestUser(sellerUsername, "sellerPass");

        Item item = new Item(sellerUsername, name, "Test description", "Cat", "Type");
        itemDao.create(item);
        createdItemIds.add(item.getId());

        Auction auction = new Auction(
            sellerUsername, 
            item.getId(), 
            1000.0, 
            LocalDateTime.now().minusHours(1), 
            LocalDateTime.now().plusHours(1)
        );
        auction.setAuctionName(name);
        auction.setDescription("Test description");
        auction.setMinIncrement(100.0);
        auction.setStatus(status);
        
        auctionDao.create(auction);
        createdAuctionIds.add(auction.getId());
        
        if (status == AuctionStatus.ACTIVE || status == AuctionStatus.UPCOMING) {
            RealtimeDatabase.addRuntimeAuction(auction);
        }
        
        return auction;
    }

    // dùng để unique username
    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    // dùng để session client
    private TestClientHandler sessionClient(User user) {
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(user.getUsername());
        RealtimeDatabase.addActiveUser(client, user);
        return client;
    }

    @Test
    void settleAuctionWithWinnerMovesToAwaitingPaymentAndDoesNotChargeWinner() {
        Auction auction = createTestAuction("Settle Auction Winner Test", AuctionStatus.ACTIVE);
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createFundedActiveUser(bidderUsername, 5000.0);
        bidder.getWallet().lockBalance(1200.0);
        userDao.save(bidder, false);

        auction.setCurrentBid(1200.0);
        auction.setCurrentBidderUsername(bidderUsername);
        auctionDao.save(auction);

        auctionService.settleAuction(auction);

        Auction updated = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.AWAITING_PAYMENT, updated.getStatus());

        User updatedBidder = ServiceUtil.getOrLoadUser(bidderUsername);
        assertEquals(5000.0, updatedBidder.getWallet().getBalance());
        assertEquals(1200.0, updatedBidder.getWallet().getLockedBalance());
    }

    @Test
    void payAuctionRequiresWinningBidder() {
        Auction auction = createTestAuction("Pay Auction Auth Test", AuctionStatus.ACTIVE);
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createFundedActiveUser(bidderUsername, 5000.0);
        bidder.getWallet().lockBalance(1200.0);
        userDao.save(bidder, false);

        auction.setCurrentBid(1200.0);
        auction.setCurrentBidderUsername(bidderUsername);
        auction.setStatus(AuctionStatus.AWAITING_PAYMENT);
        auctionDao.save(auction);

        String otherUsername = uniqueUsername("other");
        User other = createFundedActiveUser(otherUsername, 5000.0);

        TestClientHandler otherClient = sessionClient(other);

        Response response = auctionService.payAuction(otherClient, new Request(RequestType.PAY_AUCTION, new PayAuctionRequest(auction.getId())));
        assertEquals(RequestStatus.FAILED, response.getStatus());
    }

    @Test
    void payAuctionMovesToAwaitingDeliveryAndConsumesLockedBalance() {
        Auction auction = createTestAuction("Pay Auction Main Test", AuctionStatus.ACTIVE);
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createFundedActiveUser(bidderUsername, 5000.0);
        bidder.getWallet().lockBalance(1200.0);
        userDao.save(bidder, false);

        auction.setCurrentBid(1200.0);
        auction.setCurrentBidderUsername(bidderUsername);
        auction.setStatus(AuctionStatus.AWAITING_PAYMENT);
        auctionDao.save(auction);

        TestClientHandler bidderClient = sessionClient(bidder);

        Response response = auctionService.payAuction(bidderClient, new Request(RequestType.PAY_AUCTION, new PayAuctionRequest(auction.getId())));
        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        Auction updated = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.AWAITING_DELIVERY, updated.getStatus());

        User updatedWinner = userDao.findByUsername(bidderUsername);
        assertEquals(3800.0, updatedWinner.getWallet().getBalance());
        assertEquals(0.0, updatedWinner.getWallet().getLockedBalance());
    }

    @Test
    void confirmDeliveryRequiresSellerOrAdmin() {
        Auction auction = createTestAuction("Confirm Delivery Auth Test", AuctionStatus.ACTIVE);
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createFundedActiveUser(bidderUsername, 5000.0);
        bidder.getWallet().lockBalance(1200.0);
        userDao.save(bidder, false);

        auction.setCurrentBid(1200.0);
        auction.setCurrentBidderUsername(bidderUsername);
        auction.setStatus(AuctionStatus.AWAITING_DELIVERY);
        auctionDao.save(auction);

        String otherUsername = uniqueUsername("other");
        User other = createFundedActiveUser(otherUsername, 5000.0);
        TestClientHandler otherClient = sessionClient(other);

        Response response = auctionService.confirmAuctionDelivery(otherClient, new Request(RequestType.CONFIRM_AUCTION_DELIVERY, new ConfirmDeliveryRequest(auction.getId())));
        assertEquals(RequestStatus.FAILED, response.getStatus());
    }

    @Test
    void confirmDeliveryMovesItemToWinnerAndPaysSeller() {
        Auction auction = createTestAuction("Confirm Delivery Main Test", AuctionStatus.ACTIVE);
        String sellerUsername = auction.getSellerUsername();
        User seller = userDao.findByUsername(sellerUsername);
        double initialSellerBalance = seller.getWallet().getBalance();

        String bidderUsername = uniqueUsername("bidder");
        User bidder = createFundedActiveUser(bidderUsername, 5000.0);
        bidder.getWallet().lockBalance(1200.0);
        userDao.save(bidder, false);

        auction.setCurrentBid(1200.0);
        auction.setCurrentBidderUsername(bidderUsername);
        auction.setStatus(AuctionStatus.AWAITING_DELIVERY);
        auctionDao.save(auction);

        TestClientHandler sellerClient = sessionClient(seller);

        Response response = auctionService.confirmAuctionDelivery(sellerClient, new Request(RequestType.CONFIRM_AUCTION_DELIVERY, new ConfirmDeliveryRequest(auction.getId())));
        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        Auction updated = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.COMPLETED, updated.getStatus());

        User updatedSeller = userDao.findByUsername(sellerUsername);
        assertEquals(initialSellerBalance + 1200.0, updatedSeller.getWallet().getBalance());

        Item item = itemDao.findById(auction.getItemId());
        assertEquals(bidderUsername, item.getOwnerUsername());
    }

    @Test
    void adminCancelAwaitingPaymentUnlocksWinnerAndCancelsAuction() {
        Auction auction = createTestAuction("Admin Cancel Test", AuctionStatus.ACTIVE);
        String bidderUsername = uniqueUsername("bidder");
        User bidder = createFundedActiveUser(bidderUsername, 5000.0);
        bidder.getWallet().lockBalance(1200.0);
        userDao.save(bidder, false);

        auction.setCurrentBid(1200.0);
        auction.setCurrentBidderUsername(bidderUsername);
        auction.setStatus(AuctionStatus.AWAITING_PAYMENT);
        auctionDao.save(auction);

        String adminUsername = uniqueUsername("admin");
        User admin = createTestUser(adminUsername, "adminPass");
        admin.setRole(UserRole.ADMIN);
        userDao.save(admin, false);

        TestClientHandler adminClient = sessionClient(admin);

        Response response = auctionService.resolveAuction(adminClient, new Request(RequestType.RESOLVE_AUCTION, new ResolveAuctionRequest(auction.getId(), AuctionResolutionAction.CANCEL)));
        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        Auction updated = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.CANCELED, updated.getStatus());

        User updatedWinner = ServiceUtil.getOrLoadUser(bidderUsername);
        assertEquals(5000.0, updatedWinner.getWallet().getBalance());
        assertEquals(0.0, updatedWinner.getWallet().getLockedBalance());
    }

    // --- Mock Classes ---
    
    private static class TestClientHandler extends ClientHandler {
        // dùng để test client trình xử lý
        TestClientHandler() {
            // dùng để super
            super(null);
        }

        // dùng để kiểm tra xem trong phiên làm việc
        @Override
        public boolean isInSession() {
            return getCurrentUsername() != null;
        }
    }
}

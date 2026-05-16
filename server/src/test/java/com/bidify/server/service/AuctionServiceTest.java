package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.JoinAuctionRequest;
import com.bidify.common.model.LeaveAuctionRequest;
import com.bidify.common.model.PlaceBidRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;

class AuctionServiceTest {
    private final AuctionService auctionService = AuctionService.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final List<String> createdUsernames = new ArrayList<>();
    private final List<String> createdItemIds = new ArrayList<>();
    private final List<String> createdAuctionIds = new ArrayList<>();

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
    }

    @AfterEach
    void tearDown() {
        RealtimeDatabase.clearAll();
        for (String auctionId : createdAuctionIds)
            deleteAuctionData(auctionId);
        for (String itemId : createdItemIds)
            SQLiteHelper.update("DELETE FROM Items WHERE id = ?", itemId);
        for (String username : createdUsernames)
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
    }

    @Test
    void createAuctionSuccess() {
        String sellerName = uniqueUsername("seller");
        User seller = createUser(sellerName, sellerName, "password123");
        seller.getWallet().deposit(1000);
        userDao.save(seller, false);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(sellerName);
        RealtimeDatabase.addActiveUser(client, seller);
        Item item = createItem(sellerName, "Test Auction", "Test description", "General", "Electronics");

        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        LocalDateTime end = start.plusHours(2);

        Request request = new Request(
            RequestType.CREATE_AUCTION,
            new CreateAuctionRequest(
                sellerName,
                item.getId(),
                100.0,
                10.0,
                start.toString(),
                end.toString()
            )
        );

        Response response = auctionService.create(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        List<Auction> allRuntime = RealtimeDatabase.getAllRuntimeAuctions();
        assertEquals(1, allRuntime.size());

        Auction saved = allRuntime.get(0);
        assertEquals("Test Auction", saved.getAuctionName());
        assertEquals(sellerName, saved.getSellerUsername());
        assertEquals(AuctionStatus.UPCOMING, saved.getStatus());
        assertNotNull(auctionDao.findById(saved.getId()));

        createdAuctionIds.add(saved.getId());
    }

    @Test
    void createAuctionFailsWhenSellerMismatch() {
        String sellerName = uniqueUsername("seller");
        String otherName = uniqueUsername("other");
        createUser(sellerName, sellerName, "password123");
        User other = createUser(otherName, otherName, "password123");
        other.getWallet().deposit(1000);
        userDao.save(other, false);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(otherName);
        RealtimeDatabase.addActiveUser(client, other);
        Item item = createItem(sellerName, "Test Auction", "Test description", "General", "Electronics");

        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        LocalDateTime end = start.plusHours(2);

        Request request = new Request(
            RequestType.CREATE_AUCTION,
            new CreateAuctionRequest(
                sellerName,
                item.getId(),
                100.0,
                10.0,
                start.toString(),
                end.toString()
            )
        );

        Response response = auctionService.create(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("You do not own this item", response.getMessage());
    }

    @Test
    void placeBidSuccess() {
        String sellerName = uniqueUsername("seller");
        String buyerName = uniqueUsername("buyer");
        createUser(sellerName, sellerName, "password123");
        User buyer = createUser(buyerName, buyerName, "password123");
        buyer.getWallet().deposit(200.0);
        userDao.save(buyer, false);

        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        Auction auction = createAuction(sellerName, start, end);

        TestClientHandler buyerClient = new TestClientHandler();
        buyerClient.setCurrentUsername(buyerName);
        RealtimeDatabase.addActiveUser(buyerClient, buyer);

        Request request = new Request(RequestType.PLACE_BID, new PlaceBidRequest(auction.getId(), 110.0));
        Response response = auctionService.placeBid(buyerClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        Auction liveAuction = RealtimeDatabase.getLiveAuction(auction.getId());
        assertNotNull(liveAuction);
        assertEquals(110.0, liveAuction.getCurrentBid());
        assertEquals(buyerName, liveAuction.getCurrentBidderUsername());
        assertEquals(110.0, buyer.getWallet().getLockedBalance());
    }

    @Test
    void placeBidFailsWhenSellerBidsOwnAuction() {
        String sellerName = uniqueUsername("seller");
        User seller = createUser(sellerName, sellerName, "password123");
        seller.getWallet().deposit(200.0);
        userDao.save(seller, false);

        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        Auction auction = createAuction(sellerName, start, end);

        TestClientHandler sellerClient = new TestClientHandler();
        sellerClient.setCurrentUsername(sellerName);
        RealtimeDatabase.addActiveUser(sellerClient, seller);

        Request request = new Request(RequestType.PLACE_BID, new PlaceBidRequest(auction.getId(), 110.0));
        Response response = auctionService.placeBid(sellerClient, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("You cannot bid on your own auction", response.getMessage());
    }

    @Test
    void placeBidFailsWhenInsufficientBalance() {
        String sellerName = uniqueUsername("seller");
        String buyerName = uniqueUsername("buyer");
        createUser(sellerName, sellerName, "password123");
        User buyer = createUser(buyerName, buyerName, "password123");
        buyer.getWallet().deposit(50.0);
        userDao.save(buyer, false);

        LocalDateTime start = LocalDateTime.now().minusMinutes(1);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        Auction auction = createAuction(sellerName, start, end);

        TestClientHandler buyerClient = new TestClientHandler();
        buyerClient.setCurrentUsername(buyerName);
        RealtimeDatabase.addActiveUser(buyerClient, buyer);

        Request request = new Request(RequestType.PLACE_BID, new PlaceBidRequest(auction.getId(), 110.0));
        Response response = auctionService.placeBid(buyerClient, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Insufficient balance", response.getMessage());
    }

    @Test
    void settleAuctionWithWinner() {
        String sellerName = uniqueUsername("seller");
        String winnerName = uniqueUsername("winner");
        createUser(sellerName, sellerName, "password123");
        User winner = createUser(winnerName, winnerName, "password123");
        winner.getWallet().deposit(150.0);
        winner.getWallet().setlockedBalance(150.0);
        userDao.save(winner, false);

        User seller = userDao.findByUsername(sellerName);

        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusMinutes(1);
        Auction auction = createAuction(sellerName, start, end);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentBid(150.0);
        auction.setCurrentBidderUsername(winnerName);
        auctionDao.save(auction);

        auctionService.settleAuction(auction);

        Auction loadedAuction = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.ENDED, loadedAuction.getStatus());

        User loadedWinner = userDao.findByUsername(winnerName);
        User loadedSeller = userDao.findByUsername(sellerName);

        assertEquals(0.0, loadedWinner.getWallet().getBalance());
        assertEquals(0.0, loadedWinner.getWallet().getLockedBalance());
        assertEquals(150.0, loadedSeller.getWallet().getBalance());
    }

    @Test
    void settleAuctionWithoutBids() {
        String sellerName = uniqueUsername("seller");
        createUser(sellerName, sellerName, "password123");

        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusMinutes(1);
        Auction auction = createAuction(sellerName, start, end);
        auction.setStatus(AuctionStatus.ACTIVE);
        auctionDao.save(auction);

        auctionService.settleAuction(auction);

        Auction loadedAuction = auctionDao.findById(auction.getId());
        assertEquals(AuctionStatus.CANCELED, loadedAuction.getStatus());
    }

    @Test
    void joinAndLeaveAuctionSuccess() {
        String sellerName = uniqueUsername("seller");
        String watcherName = uniqueUsername("watcher");
        createUser(sellerName, sellerName, "password123");
        User watcher = createUser(watcherName, watcherName, "password123");
        watcher.getWallet().deposit(100.0);
        userDao.save(watcher, false);

        LocalDateTime start = LocalDateTime.now().plusMinutes(5);
        LocalDateTime end = start.plusHours(1);
        Auction auction = createAuction(sellerName, start, end);

        TestClientHandler watcherClient = new TestClientHandler();
        watcherClient.setCurrentUsername(watcherName);
        RealtimeDatabase.addActiveUser(watcherClient, watcher);

        Request joinRequest = new Request(RequestType.JOIN_AUCTION, new JoinAuctionRequest(auction.getId()));
        Response joinResponse = auctionService.join(watcherClient, joinRequest);

        assertEquals(RequestStatus.SUCCESS, joinResponse.getStatus());
        assertTrue(RealtimeDatabase.isWatchingAuction(watcherName, auction.getId()));

        Request leaveRequest = new Request(RequestType.LEAVE_AUCTION, new LeaveAuctionRequest(auction.getId()));
        Response leaveResponse = auctionService.leave(watcherClient, leaveRequest);

        assertEquals(RequestStatus.SUCCESS, leaveResponse.getStatus());
        assertFalse(RealtimeDatabase.isWatchingAuction(watcherName, auction.getId()));
    }

    private User createUser(String username, String nickname, String rawPassword) {
        User user = new User(username, nickname, PasswordUtil.hash(rawPassword));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    private Item createItem(String ownerUsername, String name, String description, String category, String productType) {
        Item item = new Item(ownerUsername, name, description, category, productType);
        itemDao.create(item);
        createdItemIds.add(item.getId());
        return item;
    }

    private Auction createAuction(String sellerUsername, LocalDateTime start, LocalDateTime end) {
        String auctionName = "Auction " + uniqueUsername("title");
        Item item = createItem(sellerUsername, auctionName, "Description", "General", "Electronics");
        Auction auction = new Auction(sellerUsername, item.getId(), 100.0, start, end);
        auction.setAuctionName(auctionName);
        auction.setDescription("Description");
        auction.setMinIncrement(10.0);

        if (!start.isAfter(LocalDateTime.now())) {
            auction.setStatus(AuctionStatus.ACTIVE);
        }
        
        auctionDao.create(auction);
        RealtimeDatabase.addRuntimeAuction(auction);
        createdAuctionIds.add(auction.getId());
        return auction;
    }

    private void deleteAuctionData(String auctionId) {
        SQLiteHelper.update("DELETE FROM AuctionImages WHERE auctionId = ?", auctionId);
        SQLiteHelper.update("DELETE FROM Bids WHERE auctionId = ?", auctionId);
        SQLiteHelper.update("DELETE FROM Transactions WHERE auctionId = ?", auctionId);
        SQLiteHelper.update("DELETE FROM Auctions WHERE id = ?", auctionId);
    }

    private String uniqueUsername(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return prefix.replaceAll("[^A-Za-z0-9]", "") + suffix;
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

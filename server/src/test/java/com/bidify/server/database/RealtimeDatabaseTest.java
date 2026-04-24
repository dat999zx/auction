package com.bidify.server.database;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.model.Auction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

public class RealtimeDatabaseTest {
    @AfterEach
    void cleanup() {
        RealtimeDatabase.clearAll();
    }

    @Test
    void addActiveUserShouldStoreUserAndClient() {
        ClientHandler client = new ClientHandler(null);
        client.setCurrentUsername("test");
        User user = new User("test", "test nickname", "hashed");

        RealtimeDatabase.addActiveUser(client, user);

        assertTrue(RealtimeDatabase.isUserOnline("test"));
        assertSame(client, RealtimeDatabase.getUserClient("test"));
        assertSame(user, RealtimeDatabase.getActiveUser("test"));
        assertNotNull(RealtimeDatabase.getUserSession("test"));
    }

    @Test
    void addRuntimeAuctionShouldStoreUpcomingAuction() {
        Auction auction = new Auction(
            "test",
            "testing auction",
            "seller",
            1000,
            LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusDays(1)
        );

        RealtimeDatabase.addRuntimeAuction(auction);

        assertSame(auction, RealtimeDatabase.getRuntimeAuction(auction.getId()));
        assertNotNull(RealtimeDatabase.getAuctionChannel(auction.getId()));
        assertEquals(1, RealtimeDatabase.getAllRuntimeAuctions().size());
        assertEquals(1, RealtimeDatabase.getRuntimeAuctionsByStatus(AuctionStatus.UPCOMING).size());
    }

    @Test
    void subscribeAuctionChannelShouldMarkUserWatchingAuction() {
        ClientHandler client = new ClientHandler(null);
        client.setCurrentUsername("user");
        User user = new User("user", "test user", "hashed");
        Auction auction = new Auction(
            "test",
            "testing auction",
            "seller",
            1000,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1)
        );

        RealtimeDatabase.addActiveUser(client, user);
        RealtimeDatabase.addRuntimeAuction(auction);
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), "user");

        assertTrue(RealtimeDatabase.isWatchingAuction("user", auction.getId()));
    }

    @Test
    void removeActiveUserShouldUnsubscribeUserFromAuctionChannel() {
        ClientHandler client = new ClientHandler(null);
        client.setCurrentUsername("user");
        User user = new User("user", "test user", "hashed");
        Auction auction = new Auction(
            "test",
            "testing auction",
            "seller",
            1000,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1)
        );

        RealtimeDatabase.addActiveUser(client, user);
        RealtimeDatabase.addRuntimeAuction(auction);
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), "user");

        RealtimeDatabase.removeActiveUser("user");

        assertFalse(RealtimeDatabase.isUserOnline("user"));
        assertNull(RealtimeDatabase.getUserClient("user"));
        assertFalse(RealtimeDatabase.isWatchingAuction("user", auction.getId()));
    }
}

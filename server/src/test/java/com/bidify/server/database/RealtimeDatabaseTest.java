package com.bidify.server.database;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.bidify.server.model.Auction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

public class RealtimeDatabaseTest {
    @AfterEach // tự động chạy sau mỗi test
    void cleanup() { // reset lại sau mỗi test
        RealtimeDatabase.clearAll();
    }

    @Test
    void addActiveUserShouldStoreUserAndClient() { // test thêm user và client
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
    void addLiveAuction() { // test thêm live auction
        Auction auction = new Auction("seller", "test", "testing auction", 1000, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        RealtimeDatabase.addLiveAuction(auction);

        assertSame(auction, RealtimeDatabase.getLiveAuction(auction.getId()));
        assertNotNull(RealtimeDatabase.getAuctionChannel(auction.getId()));
        assertEquals(1, RealtimeDatabase.getAllLiveAuctions().size());
    }

    @Test
    void testSubscribedUserWatchingAuction() { // user đã subscribe vào auction đang xem auction
        ClientHandler client = new ClientHandler(null);
        client.setCurrentUsername("user");
        User user = new User("user", "test user", "hashed");
        Auction auction = new Auction("seller", "test", "testing auction", 1000, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        RealtimeDatabase.addActiveUser(client, user);
        RealtimeDatabase.addLiveAuction(auction);
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), "user");

        assertTrue(RealtimeDatabase.isWatchingAuction("user", auction.getId()));
    }

    @Test
    void removeActiveUser() { // xóa user
        ClientHandler client = new ClientHandler(null);
        client.setCurrentUsername("user");
        User user = new User("user", "test user", "hashed");
        Auction auction = new Auction("seller", "test", "testing auction", 1000, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        RealtimeDatabase.addActiveUser(client, user);
        RealtimeDatabase.addLiveAuction(auction);
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), "alice");

        RealtimeDatabase.removeActiveUser("user");

        assertFalse(RealtimeDatabase.isUserOnline("user"));
        assertNull(RealtimeDatabase.getUserClient("user"));
        assertFalse(RealtimeDatabase.isWatchingAuction("user", auction.getId()));
    }
}

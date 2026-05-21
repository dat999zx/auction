package com.bidify.server.database;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import com.bidify.common.utility.TimeUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.model.Auction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

public class RealtimeDatabaseTest {
    // dùng để dọn dẹp tài nguyên
    @AfterEach
    void cleanup() {
        RealtimeDatabase.clearAll();
    }

    // dùng để thêm active người dùng should store người dùng and client
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

    // dùng để thêm runtime đấu giá should store upcoming đấu giá
    @Test
    void addRuntimeAuctionShouldStoreUpcomingAuction() {
        Auction auction = new Auction(
            "test",
            "testing auction",
            "seller",
            1000,
            TimeUtil.nowInVietnam().plusHours(1),
            TimeUtil.nowInVietnam().plusDays(1)
        );

        RealtimeDatabase.addRuntimeAuction(auction);

        assertSame(auction, RealtimeDatabase.getRuntimeAuction(auction.getId()));
        assertNotNull(RealtimeDatabase.getAuctionChannel(auction.getId()));
        assertEquals(1, RealtimeDatabase.getAllRuntimeAuctions().size());
        assertEquals(1, RealtimeDatabase.getRuntimeAuctionsByStatus(AuctionStatus.UPCOMING).size());
    }

    // dùng để đăng ký lắng nghe sự kiện đấu giá kênh truyền tải should mark người dùng watching đấu giá
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
            TimeUtil.nowInVietnam(),
            TimeUtil.nowInVietnam().plusDays(1)
        );

        RealtimeDatabase.addActiveUser(client, user);
        RealtimeDatabase.addRuntimeAuction(auction);
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), "user");

        assertTrue(RealtimeDatabase.isWatchingAuction("user", auction.getId()));
    }

    // dùng để xóa active người dùng should hủy đăng ký lắng nghe sự kiện người dùng từ đấu giá kênh truyền tải
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
            TimeUtil.nowInVietnam(),
            TimeUtil.nowInVietnam().plusDays(1)
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

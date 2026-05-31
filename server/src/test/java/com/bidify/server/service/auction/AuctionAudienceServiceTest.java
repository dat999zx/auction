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
import com.bidify.common.model.JoinAuctionRequest;
import com.bidify.common.model.LeaveAuctionRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

/**
 * Lớp kiểm thử (Unit Test) cho AuctionAudienceService.
 * Bình luận mã nguồn bằng tiếng Việt.
 */
public class AuctionAudienceServiceTest {

    private final UserDao userDao = UserDao.getInstance();
    private AuctionAudienceService audienceService;

    @BeforeAll
    public static void setUpClass() {
        SQLiteHelper.init();
    }

    @BeforeEach
    public void setUp() {
        audienceService = new AuctionAudienceService(new AuctionDtoAssembler(), new AuctionRealtimePublisher());
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
        SQLiteHelper.update("DELETE FROM Auctions");
    }

    /**
     * Ca kiểm thử: Người dùng tham gia (join) vào theo dõi đấu giá thành công.
     */
    @Test
    public void testJoinAuctionSuccessfully() {
        String username = "watcher_1";
        User user = new User(username, "Watcher One", "pass");
        userDao.create(user);

        // Tạo đấu giá hoạt động (ACTIVE)
        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-join-ok", now, "Auction Title", "Desc", "seller_usr", "item-1",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        // Giả lập ClientHandler đang online
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        JoinAuctionRequest joinData = new JoinAuctionRequest(auction.getId());
        Request request = new Request(RequestType.JOIN_AUCTION, joinData);

        Response response = audienceService.join(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Join auction successfully", response.getMessage());

        // Kiểm tra xem đã subscribe vào channel thành công hay chưa
        assertTrue(RealtimeDatabase.isWatchingAuction(username, auction.getId()));
        assertEquals(1, RealtimeDatabase.getAuctionChannel(auction.getId()).getObserverCount());
    }

    /**
     * Ca kiểm thử: Tham gia trùng lặp (duplicate join) không tăng watcher và xử lý an toàn.
     */
    @Test
    public void testDuplicateJoinAuction() {
        String username = "watcher_dup";
        User user = new User(username, "Watcher Dup", "pass");
        userDao.create(user);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-join-dup", now, "Auction Title", "Desc", "seller_usr", "item-2",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        JoinAuctionRequest joinData = new JoinAuctionRequest(auction.getId());
        Request request = new Request(RequestType.JOIN_AUCTION, joinData);

        // Join lần đầu
        Response response1 = audienceService.join(client, request);
        assertEquals(RequestStatus.SUCCESS, response1.getStatus());

        // Join lần hai
        Response response2 = audienceService.join(client, request);
        assertEquals(RequestStatus.SUCCESS, response2.getStatus());
        assertEquals("You are already watching this auction", response2.getMessage());

        // Đảm bảo số lượng observer vẫn là 1
        assertEquals(1, RealtimeDatabase.getAuctionChannel(auction.getId()).getObserverCount());
    }

    /**
     * Ca kiểm thử: Người xem rời (leave) khỏi kênh theo dõi đấu giá.
     */
    @Test
    public void testLeaveAuctionSuccessfully() {
        String username = "watcher_leave";
        User user = new User(username, "Watcher Leave", "pass");
        userDao.create(user);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-leave-ok", now, "Auction Title", "Desc", "seller_usr", "item-3",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Cho join trước
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), username);
        assertTrue(RealtimeDatabase.isWatchingAuction(username, auction.getId()));

        // Thực hiện leave
        LeaveAuctionRequest leaveData = new LeaveAuctionRequest(auction.getId());
        Request request = new Request(RequestType.LEAVE_AUCTION, leaveData);

        Response response = audienceService.leave(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Leave auction successfully", response.getMessage());

        // Kiểm tra xem đã unsubscribe thành công
        assertFalse(RealtimeDatabase.isWatchingAuction(username, auction.getId()));
        assertEquals(0, RealtimeDatabase.getAuctionChannel(auction.getId()).getObserverCount());
    }

    /**
     * Ca kiểm thử: Rời khỏi phiên đấu giá khi chưa xem (leave without join) -> Ném lỗi.
     */
    @Test
    public void testLeaveAuctionFailsWhenNotWatching() {
        String username = "watcher_not_join";
        User user = new User(username, "Watcher Not Join", "pass");
        userDao.create(user);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-leave-fail", now, "Auction Title", "Desc", "seller_usr", "item-4",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        LeaveAuctionRequest leaveData = new LeaveAuctionRequest(auction.getId());
        Request request = new Request(RequestType.LEAVE_AUCTION, leaveData);

        Response response = audienceService.leave(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertTrue(response.getMessage().contains("You are not watching"));
    }

    /**
     * Ca kiểm thử: Đấu giá không tồn tại hoặc đã kết thúc (không ở trạng thái runtime) -> Lỗi khi join.
     */
    @Test
    public void testJoinInvalidOrExpiredAuction() {
        String username = "watcher_invalid";
        User user = new User(username, "Watcher Invalid", "pass");
        userDao.create(user);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // 1. Phiên đấu giá không tồn tại
        JoinAuctionRequest joinDataNonExistent = new JoinAuctionRequest("non-existent-id");
        Request request1 = new Request(RequestType.JOIN_AUCTION, joinDataNonExistent);
        Response response1 = audienceService.join(client, request1);
        assertEquals(RequestStatus.FAILED, response1.getStatus());

        // 2. Phiên đấu giá đã kết thúc (COMPLETED) - không thuộc in-memory runtime
        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction expiredAuction = new Auction(
            "auc-expired", now, "Title", "Desc", "seller", "item-5",
            null, 100.0, 10.0, now.minusDays(2), now.minusDays(1), AuctionStatus.COMPLETED
        );
        // Không đưa vào runtime hoặc nếu đưa vào thì isRuntimeAuction() sẽ trả về false
        RealtimeDatabase.addRuntimeAuction(expiredAuction); // Hàm addRuntimeAuction sẽ tự kiểm tra trạng thái và bỏ qua nếu không phải UPCOMING/ACTIVE

        JoinAuctionRequest joinDataExpired = new JoinAuctionRequest(expiredAuction.getId());
        Request request2 = new Request(RequestType.JOIN_AUCTION, joinDataExpired);
        Response response2 = audienceService.join(client, request2);
        assertEquals(RequestStatus.FAILED, response2.getStatus());
        assertTrue(response2.getMessage().contains("Auction not found"));
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

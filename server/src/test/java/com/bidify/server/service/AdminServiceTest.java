package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.dto.AdminUserDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;
import com.bidify.common.model.Event;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UserTargetRequest;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) cho {@link AdminService}.
 * Kiểm tra các tính năng quản trị của hệ thống bao gồm:
 * - Xem danh sách người dùng.
 * - Cấm (Ban) và gỡ cấm (Unban) người dùng.
 * - Xóa tài khoản người dùng và cascade xóa toàn bộ dữ liệu liên quan (Đấu giá, Sản phẩm, Bids, Giao dịch, Ảnh).
 * - Thăng chức (Promote) và bãi nhiệm (Demote) Admin bởi Bootstrap Admin.
 * - Bảo vệ tài khoản Bootstrap Admin không thể bị chỉnh sửa/xóa.
 * Có chú thích chi tiết bằng tiếng Việt.
 */
class AdminServiceTest {

    private final AdminService adminService = AdminService.getInstance();

    private final UserDao userDao = UserDao.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();

    // Theo dõi tài nguyên thử nghiệm để xóa sạch sau khi kiểm thử xong
    private final List<String> createdUsernames = new ArrayList<>();
    private final List<String> createdItemIds = new ArrayList<>();
    private final List<String> createdAuctionIds = new ArrayList<>();
    private final List<String> createdImageIds = new ArrayList<>();

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
        createdImageIds.clear();
    }

    @AfterEach
    void tearDown() {
        RealtimeDatabase.clearAll();

        // Xóa Bids trước vì nó liên kết khóa ngoại với Auctions và Users
        for (String auctionId : createdAuctionIds) {
            SQLiteHelper.update("DELETE FROM Bids WHERE auctionId = ?", auctionId);
            SQLiteHelper.update("DELETE FROM Transactions WHERE auctionId = ?", auctionId);
            SQLiteHelper.update("DELETE FROM Auctions WHERE id = ?", auctionId);
        }

        // Xóa nốt các Bids hoặc Transactions mồ côi (nếu có)
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Bids WHERE bidder = ?", username);
            SQLiteHelper.update("DELETE FROM Transactions WHERE username = ?", username);
        }

        // Xóa liên kết ảnh và sản phẩm
        for (String itemId : createdItemIds) {
            SQLiteHelper.update("DELETE FROM ItemImageLinks WHERE itemId = ?", itemId);
            SQLiteHelper.update("DELETE FROM Items WHERE id = ?", itemId);
        }

        // Xóa người dùng
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }

        // Xóa hình ảnh
        for (String imageId : createdImageIds) {
            SQLiteHelper.update("DELETE FROM Images WHERE id = ?", imageId);
        }
    }

    /**
     * Ca kiểm thử: Admin lấy danh sách người dùng thành công.
     */
    @Test
    void getUsersSuccessfully() {
        // Tạo tài khoản admin và một tài khoản thường làm mẫu
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String userUsername = uniqueUsername("user");
        createUser(userUsername, "UserNick", "userPass");

        // Giả lập admin online
        TestClientHandler adminClient = sessionClient(adminUser);

        Response response = adminService.getUsers(adminClient);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Users loaded successfully", response.getMessage());

        assertNotNull(response.getData());
        List<AdminUserDto> list = (List<AdminUserDto>) response.getData();
        assertFalse(list.isEmpty());

        // Kiểm tra xem user vừa tạo có trong danh sách không
        boolean foundUser = list.stream().anyMatch(dto -> dto.getUsername().equals(userUsername));
        assertTrue(foundUser);
    }

    /**
     * Ca kiểm thử: Lấy danh sách người dùng thất bại khi tài khoản gọi không phải Admin.
     */
    @Test
    void getUsersFailsForNonAdmin() {
        String userUsername = uniqueUsername("user");
        User normalUser = createUser(userUsername, "UserNick", "userPass");

        TestClientHandler userClient = sessionClient(normalUser);

        Response response = adminService.getUsers(userClient);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Admin permission required", response.getMessage());
    }

    /**
     * Ca kiểm thử: Admin cấm (Ban) người dùng thường thành công.
     * Xác thực: trạng thái tài khoản đổi thành BANNED và bị forced logout.
     */
    @Test
    void banUserSuccessfully() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        User targetUser = createUser(targetUsername, "TargetNick", "pass123");

        TestClientHandler adminClient = sessionClient(adminUser);
        TestClientHandler targetClient = sessionClient(targetUser);

        Request request = new Request(RequestType.BAN_USER, new UserTargetRequest(targetUsername));
        Response response = adminService.banUser(adminClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("User banned successfully", response.getMessage());

        // 1. Xác thực trạng thái trong DB đổi thành BANNED
        User updatedTarget = userDao.findByUsername(targetUsername);
        assertNotNull(updatedTarget);
        assertEquals(UserStatus.BANNED, updatedTarget.getStatus());

        // 2. Xác thực target user bị buộc đăng xuất (FORCED_LOGOUT event)
        boolean hasForcedLogout = targetClient.getSentEvents().stream()
                .anyMatch(e -> e.getType() == EventType.FORCED_LOGOUT);
        assertTrue(hasForcedLogout, "Người dùng bị cấm phải nhận sự kiện FORCED_LOGOUT.");
        assertNull(targetClient.getCurrentUsername(), "Phiên đăng nhập hiện tại của người dùng phải bị xóa bỏ.");
    }

    /**
     * Ca kiểm thử: Cấm người dùng thất bại khi người thực hiện không phải Admin.
     */
    @Test
    void banUserFailsForNonAdmin() {
        String normalUsername = uniqueUsername("user");
        User normalUser = createUser(normalUsername, "UserNick", "userPass");

        String targetUsername = uniqueUsername("target");
        createUser(targetUsername, "TargetNick", "pass123");

        TestClientHandler client = sessionClient(normalUser);

        Request request = new Request(RequestType.BAN_USER, new UserTargetRequest(targetUsername));
        Response response = adminService.banUser(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Admin permission required", response.getMessage());
    }

    /**
     * Ca kiểm thử: Admin cố tình cấm một Admin khác -> Phải thất bại.
     */
    @Test
    void banUserFailsForAdminTarget() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String otherAdminUsername = uniqueUsername("otheradmin");
        User otherAdmin = createAdminUser(otherAdminUsername, "OtherAdminNick", "otherPass");

        TestClientHandler adminClient = sessionClient(adminUser);

        Request request = new Request(RequestType.BAN_USER, new UserTargetRequest(otherAdminUsername));
        Response response = adminService.banUser(adminClient, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Cannot manage admin accounts", response.getMessage());
    }

    /**
     * Ca kiểm thử: Admin gỡ cấm (Unban) người dùng thành công.
     * Xác thực: trạng thái tài khoản chuyển từ BANNED sang ACTIVE.
     */
    @Test
    void unbanUserSuccessfully() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        User targetUser = createUser(targetUsername, "TargetNick", "pass123");
        targetUser.setStatus(UserStatus.BANNED);
        userDao.save(targetUser, false);

        TestClientHandler adminClient = sessionClient(adminUser);

        Request request = new Request(RequestType.UNBAN_USER, new UserTargetRequest(targetUsername));
        Response response = adminService.unbanUser(adminClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("User unbanned successfully", response.getMessage());

        User updatedTarget = userDao.findByUsername(targetUsername);
        assertNotNull(updatedTarget);
        assertEquals(UserStatus.ACTIVE, updatedTarget.getStatus());
    }

    /**
     * Ca kiểm thử: Gỡ cấm người dùng thất bại khi người thực hiện không phải Admin.
     */
    @Test
    void unbanUserFailsForNonAdmin() {
        String normalUsername = uniqueUsername("user");
        User normalUser = createUser(normalUsername, "UserNick", "userPass");

        String targetUsername = uniqueUsername("target");
        createUser(targetUsername, "TargetNick", "pass123");

        TestClientHandler client = sessionClient(normalUser);

        Request request = new Request(RequestType.UNBAN_USER, new UserTargetRequest(targetUsername));
        Response response = adminService.unbanUser(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Admin permission required", response.getMessage());
    }

    /**
     * Ca kiểm thử: Admin xóa tài khoản người dùng thành công.
     * Xác thực: Cascade xóa toàn bộ sản phẩm, đấu giá, ảnh sản phẩm, bids, transactions và đóng session.
     */
    @Test
    void deleteUserSuccessfully() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        User targetUser = createUser(targetUsername, "TargetNick", "pass123");

        // Tạo sản phẩm cho target user
        Item item = createTestItem(targetUsername, "Vũ khí test");
        // Tạo ảnh sản phẩm
        Image image = createTestImage("img-test-123", "uploads/test-image.png");
        // Liên kết ảnh và sản phẩm
        List<Image> images = new ArrayList<>();
        images.add(image);
        itemDao.saveItemImageLinks(item.getId(), images);

        // Tạo cuộc đấu giá cho sản phẩm của target user
        Auction auction = createTestAuction(targetUsername, item.getId(), "Đấu giá thử nghiệm");
        
        // Tạo một lượt bid từ target user lên cuộc đấu giá khác
        String otherSeller = uniqueUsername("seller");
        createUser(otherSeller, "SellerNick", "sellerPass");
        Item otherItem = createTestItem(otherSeller, "Món đồ khác");
        Auction otherAuction = createTestAuction(otherSeller, otherItem.getId(), "Đấu giá khác");
        Bid bid = createTestBid(otherAuction.getId(), targetUsername, 1500.0);

        // Tạo giao dịch ví mẫu của target user
        Transaction transaction = createTestTransaction(targetUsername, TransactionType.DEPOSIT, 500.0, null);

        // Giả lập admin và target user đều online
        TestClientHandler adminClient = sessionClient(adminUser);
        TestClientHandler targetClient = sessionClient(targetUser);

        // Thực hiện xóa user
        Request request = new Request(RequestType.DELETE_USER, new UserTargetRequest(targetUsername));
        Response response = adminService.deleteUser(adminClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("User deleted successfully", response.getMessage());

        // 1. Xác thực tài khoản bị xóa khỏi DB
        assertNull(userDao.findByUsername(targetUsername));

        // 2. Xác thực phiên làm việc bị ngắt kết nối (SERVER_NOTICE event)
        boolean hasServerNotice = targetClient.getSentEvents().stream()
                .anyMatch(e -> e.getType() == EventType.SERVER_NOTICE && e.getMessage().contains("deleted"));
        assertTrue(hasServerNotice, "Người dùng bị xóa phải nhận được thông báo SERVER_NOTICE.");
        assertNull(targetClient.getCurrentUsername(), "Username trong ClientHandler phải được set về null.");

        // 3. Xác thực sản phẩm và ảnh bị xóa cascade
        assertNull(itemDao.findById(item.getId()));
        assertTrue(itemDao.findImageIdsByItemId(item.getId()).isEmpty());
        assertNull(imageDao.findById(image.getId()));

        // 4. Xác thực cuộc đấu giá của user bị xóa cascade
        assertNull(auctionDao.findById(auction.getId()));

        // 5. Xác thực bids và transactions của user bị xóa
        assertTrue(bidDao.findByUsername(targetUsername).isEmpty());
        assertTrue(transactionDao.findByUsername(targetUsername).isEmpty());
    }

    /**
     * Ca kiểm thử: Xóa người dùng thất bại khi tài khoản gọi không phải Admin.
     */
    @Test
    void deleteUserFailsForNonAdmin() {
        String normalUsername = uniqueUsername("user");
        User normalUser = createUser(normalUsername, "UserNick", "userPass");

        String targetUsername = uniqueUsername("target");
        createUser(targetUsername, "TargetNick", "pass123");

        TestClientHandler client = sessionClient(normalUser);

        Request request = new Request(RequestType.DELETE_USER, new UserTargetRequest(targetUsername));
        Response response = adminService.deleteUser(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Admin permission required", response.getMessage());
    }

    /**
     * Ca kiểm thử: Bootstrap Admin thăng chức cho người dùng thường thành Admin thành công.
     * Xác thực: Vai trò đổi thành ADMIN và bị forced logout.
     */
    @Test
    void promoteAdminSuccessfully() {
        // Tạo Bootstrap Admin đúng tên tài khoản mặc định
        User bootstrapAdmin = new User(AuthService.BOOTSTRAP_ADMIN_USERNAME, "System Admin", PasswordUtil.hash("bootPass"));
        bootstrapAdmin.setRole(UserRole.ADMIN);
        userDao.create(bootstrapAdmin);
        createdUsernames.add(AuthService.BOOTSTRAP_ADMIN_USERNAME);

        String targetUsername = uniqueUsername("target");
        User targetUser = createUser(targetUsername, "TargetNick", "pass123");

        TestClientHandler bootClient = sessionClient(bootstrapAdmin);
        TestClientHandler targetClient = sessionClient(targetUser);

        Request request = new Request(RequestType.PROMOTE_ADMIN, new UserTargetRequest(targetUsername));
        Response response = adminService.promoteAdmin(bootClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("User promoted to admin successfully", response.getMessage());

        // 1. Vai trò đổi thành ADMIN trong DB
        User updatedTarget = userDao.findByUsername(targetUsername);
        assertNotNull(updatedTarget);
        assertEquals(UserRole.ADMIN, updatedTarget.getRole());

        // 2. Tài khoản bị buộc logout nhận thông báo
        boolean hasForcedLogout = targetClient.getSentEvents().stream()
                .anyMatch(e -> e.getType() == EventType.FORCED_LOGOUT);
        assertTrue(hasForcedLogout, "Tài khoản thăng chức phải nhận sự kiện FORCED_LOGOUT.");
    }

    /**
     * Ca kiểm thử: Thăng chức Admin thất bại khi tài khoản gọi không phải Bootstrap Admin (Dù là Admin thường).
     */
    @Test
    void promoteAdminFailsForNonBootstrapAdmin() {
        // Tạo Admin thông thường
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        createUser(targetUsername, "TargetNick", "pass123");

        TestClientHandler adminClient = sessionClient(adminUser);

        Request request = new Request(RequestType.PROMOTE_ADMIN, new UserTargetRequest(targetUsername));
        Response response = adminService.promoteAdmin(adminClient, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Bootstrap admin permission required", response.getMessage());
    }

    /**
     * Ca kiểm thử: Thăng chức Admin thất bại khi đích đến đã là Admin sẵn.
     */
    @Test
    void promoteAdminFailsIfAlreadyAdmin() {
        User bootstrapAdmin = new User(AuthService.BOOTSTRAP_ADMIN_USERNAME, "System Admin", PasswordUtil.hash("bootPass"));
        bootstrapAdmin.setRole(UserRole.ADMIN);
        userDao.create(bootstrapAdmin);
        createdUsernames.add(AuthService.BOOTSTRAP_ADMIN_USERNAME);

        String targetUsername = uniqueUsername("admin2");
        User targetAdmin = createAdminUser(targetUsername, "Admin2Nick", "pass123");

        TestClientHandler bootClient = sessionClient(bootstrapAdmin);

        Request request = new Request(RequestType.PROMOTE_ADMIN, new UserTargetRequest(targetUsername));
        Response response = adminService.promoteAdmin(bootClient, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("User is already an admin", response.getMessage());
    }

    /**
     * Ca kiểm thử: Bootstrap Admin bãi nhiệm Admin thường thành công.
     * Xác thực: Vai trò đổi thành USER và bị forced logout.
     */
    @Test
    void demoteAdminSuccessfully() {
        User bootstrapAdmin = new User(AuthService.BOOTSTRAP_ADMIN_USERNAME, "System Admin", PasswordUtil.hash("bootPass"));
        bootstrapAdmin.setRole(UserRole.ADMIN);
        userDao.create(bootstrapAdmin);
        createdUsernames.add(AuthService.BOOTSTRAP_ADMIN_USERNAME);

        String targetUsername = uniqueUsername("admin2");
        User targetAdmin = createAdminUser(targetUsername, "Admin2Nick", "pass123");

        TestClientHandler bootClient = sessionClient(bootstrapAdmin);
        TestClientHandler targetClient = sessionClient(targetAdmin);

        Request request = new Request(RequestType.DEMOTE_ADMIN, new UserTargetRequest(targetUsername));
        Response response = adminService.demoteAdmin(bootClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Admin removed successfully", response.getMessage());

        // 1. Vai trò đổi thành USER trong DB
        User updatedTarget = userDao.findByUsername(targetUsername);
        assertNotNull(updatedTarget);
        assertEquals(UserRole.USER, updatedTarget.getRole());

        // 2. Buộc đăng xuất
        boolean hasForcedLogout = targetClient.getSentEvents().stream()
                .anyMatch(e -> e.getType() == EventType.FORCED_LOGOUT);
        assertTrue(hasForcedLogout, "Tài khoản bị bãi nhiệm phải nhận sự kiện FORCED_LOGOUT.");
    }

    /**
     * Ca kiểm thử: Bãi nhiệm Admin thất bại khi tài khoản gọi không phải Bootstrap Admin.
     */
    @Test
    void demoteAdminFailsForNonBootstrapAdmin() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("admin2");
        createAdminUser(targetUsername, "Admin2Nick", "pass123");

        TestClientHandler adminClient = sessionClient(adminUser);

        Request request = new Request(RequestType.DEMOTE_ADMIN, new UserTargetRequest(targetUsername));
        Response response = adminService.demoteAdmin(adminClient, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Bootstrap admin permission required", response.getMessage());
    }

    /**
     * Ca kiểm thử: Bãi nhiệm Admin thất bại khi tài khoản được chỉ định vốn chỉ là người dùng thường.
     */
    @Test
    void demoteAdminFailsIfAlreadyUser() {
        User bootstrapAdmin = new User(AuthService.BOOTSTRAP_ADMIN_USERNAME, "System Admin", PasswordUtil.hash("bootPass"));
        bootstrapAdmin.setRole(UserRole.ADMIN);
        userDao.create(bootstrapAdmin);
        createdUsernames.add(AuthService.BOOTSTRAP_ADMIN_USERNAME);

        String targetUsername = uniqueUsername("user");
        createUser(targetUsername, "UserNick", "pass123");

        TestClientHandler bootClient = sessionClient(bootstrapAdmin);

        Request request = new Request(RequestType.DEMOTE_ADMIN, new UserTargetRequest(targetUsername));
        Response response = adminService.demoteAdmin(bootClient, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("User is not an admin", response.getMessage());
    }

    /**
     * Ca kiểm thử: Hệ thống bảo vệ Bootstrap Admin ("admin").
     * Không ai được phép Ban/Delete/Promote/Demote tài khoản này.
     */
    @Test
    void manageBootstrapAdminFails() {
        // Tạo tài khoản bootstrap admin
        User bootstrapAdmin = new User(AuthService.BOOTSTRAP_ADMIN_USERNAME, "System Admin", PasswordUtil.hash("bootPass"));
        bootstrapAdmin.setRole(UserRole.ADMIN);
        userDao.create(bootstrapAdmin);
        createdUsernames.add(AuthService.BOOTSTRAP_ADMIN_USERNAME);

        // Tạo một admin khác cố gắng cấm/xóa bootstrap admin
        String otherAdminUsername = uniqueUsername("admin");
        User otherAdmin = createAdminUser(otherAdminUsername, "AdminNick", "adminPass");

        TestClientHandler adminClient = sessionClient(otherAdmin);

        // 1. Thử Ban
        Request banRequest = new Request(RequestType.BAN_USER, new UserTargetRequest(AuthService.BOOTSTRAP_ADMIN_USERNAME));
        Response banResponse = adminService.banUser(adminClient, banRequest);
        assertEquals(RequestStatus.FAILED, banResponse.getStatus());
        assertEquals("Cannot manage the bootstrap admin account", banResponse.getMessage());

        // 2. Thử Delete
        Request delRequest = new Request(RequestType.DELETE_USER, new UserTargetRequest(AuthService.BOOTSTRAP_ADMIN_USERNAME));
        Response delResponse = adminService.deleteUser(adminClient, delRequest);
        assertEquals(RequestStatus.FAILED, delResponse.getStatus());
        assertEquals("Cannot manage the bootstrap admin account", delResponse.getMessage());
    }

    // --- Hàm trợ giúp (Helper Methods) ---

    private User createUser(String username, String nickname, String rawPassword) {
        User user = new User(username, nickname, PasswordUtil.hash(rawPassword));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    private User createAdminUser(String username, String nickname, String rawPassword) {
        User user = new User(username, nickname, PasswordUtil.hash(rawPassword));
        user.setRole(UserRole.ADMIN);
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    private Item createTestItem(String owner, String name) {
        Item item = new Item(owner, name, "Mô tả sản phẩm test", "CategoryTest", "TypeTest");
        itemDao.create(item);
        createdItemIds.add(item.getId());
        return item;
    }

    private Image createTestImage(String id, String filePath) {
        Image image = new Image(id, TimeUtil.nowInVietnam(), filePath);
        imageDao.create(image);
        createdImageIds.add(id);
        return image;
    }

    private Auction createTestAuction(String seller, String itemId, String name) {
        Auction auction = new Auction(
            seller, 
            itemId, 
            1000.0, 
            TimeUtil.nowInVietnam().minusHours(1),
            TimeUtil.nowInVietnam().plusHours(1)
        );
        auction.setAuctionName(name);
        auction.setDescription("Mô tả đấu giá test");
        auction.setMinIncrement(100.0);
        auction.setStatus(AuctionStatus.ACTIVE);
        auctionDao.create(auction);
        createdAuctionIds.add(auction.getId());

        // Thêm vào database thời gian thực để mô phỏng runtime
        RealtimeDatabase.addRuntimeAuction(auction);
        return auction;
    }

    private Bid createTestBid(String auctionId, String bidder, double amount) {
        Bid bid = new Bid(auctionId, bidder, amount, false);
        bidDao.create(bid);
        return bid;
    }

    private Transaction createTestTransaction(String username, TransactionType type, double amount, String auctionId) {
        Transaction tx = new Transaction(username, type, amount, auctionId);
        transactionDao.create(tx);
        return tx;
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private TestClientHandler sessionClient(User user) {
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(user.getUsername());
        RealtimeDatabase.addActiveUser(client, user);
        return client;
    }

    // --- Mock Client Handler ---

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

        @Override
        public void closeConnection() {
            // No-op for mock
        }

        public List<Event> getSentEvents() {
            return sentEvents;
        }
    }
}

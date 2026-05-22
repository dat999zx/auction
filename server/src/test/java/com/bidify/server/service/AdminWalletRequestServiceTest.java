package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.dto.WalletRequestDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.WalletRequestStatus;
import com.bidify.common.model.Event;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.WalletReviewRequest;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.dao.WalletRequestDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Admin;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.model.WalletRequest;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) cho {@link AdminWalletRequestService}.
 * Kiểm tra các tính năng của Admin: Xem danh sách yêu cầu nạp/rút tiền đang chờ duyệt,
 * phê duyệt (Approve) hoặc từ chối (Deny) các yêu cầu nạp/rút này.
 * Có chú thích chi tiết cho từng phương thức và ca kiểm thử.
 */
class AdminWalletRequestServiceTest {

    // Service cần kiểm thử
    private final AdminWalletRequestService adminWalletRequestService = AdminWalletRequestService.getInstance();

    // Các đối tượng DAO dùng để thao tác và xác thực dữ liệu trực tiếp trong cơ sở dữ liệu SQLite
    private final UserDao userDao = UserDao.getInstance();
    private final WalletRequestDao walletRequestDao = WalletRequestDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();

    // Theo dõi danh sách dữ liệu giả lập được tạo ra trong các test để xóa sạch sau khi kết thúc
    private final List<String> createdUsernames = new ArrayList<>();
    private final List<String> createdWalletRequestIds = new ArrayList<>();

    /**
     * Khởi tạo schema cơ sở dữ liệu SQLite trước khi thực hiện các bài test.
     */
    @BeforeAll
    static void initDatabase() {
        SQLiteHelper.init();
    }

    /**
     * Thiết lập ban đầu trước mỗi ca kiểm thử:
     * - Dọn dẹp cơ sở dữ liệu bộ nhớ tạm thời (RAM) RealtimeDatabase.
     * - Làm sạch danh sách lưu trữ tạm của các bản ghi thử nghiệm.
     */
    @BeforeEach
    void setUp() {
        RealtimeDatabase.clearAll();
        createdUsernames.clear();
        createdWalletRequestIds.clear();
    }

    /**
     * Dọn dẹp tài nguyên sau mỗi ca kiểm thử:
     * - Xóa thông tin phiên hoạt động trong RAM.
     * - Xóa toàn bộ dữ liệu người dùng, giao dịch và yêu cầu ví giả lập đã lưu trong SQLite.
     */
    @AfterEach
    void tearDown() {
        RealtimeDatabase.clearAll();

        // Xóa các giao dịch của người dùng thử nghiệm
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Transactions WHERE username = ?", username);
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }

        // Xóa các yêu cầu ví thử nghiệm
        for (String reqId : createdWalletRequestIds) {
            SQLiteHelper.update("DELETE FROM WalletRequests WHERE id = ?", reqId);
        }
    }

    /**
     * Ca kiểm thử: Lấy danh sách yêu cầu ví đang chờ duyệt thành công bởi Admin.
     */
    @Test
    void getPendingRequestsSuccessfully() {
        // Tạo tài khoản admin và một yêu cầu nạp tiền giả lập
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        createUser(targetUsername, "TargetNick", "pass123");

        WalletRequest pendingReq = createWalletRequest(targetUsername, TransactionType.DEPOSIT, 150.0);

        // Giả lập client đăng nhập với tài khoản Admin
        TestClientHandler adminClient = new TestClientHandler();
        adminClient.setCurrentUsername(adminUsername);
        RealtimeDatabase.addActiveUser(adminClient, adminUser);

        // Thực hiện lấy danh sách chờ duyệt
        Response response = adminWalletRequestService.getPendingRequests(adminClient);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Pending requests loaded", response.getMessage());

        // Kiểm tra xem danh sách trả về có chứa yêu cầu nạp tiền vừa tạo hay không
        assertNotNull(response.getData());
        List<WalletRequestDto> list = (List<WalletRequestDto>) response.getData();
        assertFalse(list.isEmpty());

        boolean found = list.stream().anyMatch(dto -> dto.getId().equals(pendingReq.getId()));
        assertTrue(found, "Yêu cầu ví vừa tạo phải có mặt trong danh sách chờ duyệt.");
    }

    /**
     * Ca kiểm thử: Lấy danh sách yêu cầu ví thất bại khi tài khoản gọi không phải Admin.
     */
    @Test
    void getPendingRequestsFailsForNormalUser() {
        // Tạo tài khoản người dùng bình thường (User Role: USER)
        String username = uniqueUsername("user");
        User normalUser = createUser(username, "UserNick", "userPass");

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, normalUser);

        Response response = adminWalletRequestService.getPendingRequests(client);

        // Mong đợi kết quả thất bại vì không có quyền Admin
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Admin permission required", response.getMessage());
    }

    /**
     * Ca kiểm thử: Lấy danh sách yêu cầu ví thất bại khi phiên làm việc không hợp lệ (Chưa đăng nhập).
     */
    @Test
    void getPendingRequestsFailsForInvalidSession() {
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(null); // Chưa đăng nhập

        Response response = adminWalletRequestService.getPendingRequests(client);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Invalid session", response.getMessage());
    }

    /**
     * Ca kiểm thử: Admin phê duyệt (Approve) yêu cầu nạp tiền (DEPOSIT) thành công.
     * Xác thực:
     * - Số dư ví của người dùng được cộng thêm.
     * - Bản ghi giao dịch (Transaction) được tạo.
     * - Trạng thái yêu cầu ví chuyển thành APPROVED.
     * - Sự kiện thay đổi số dư và danh sách yêu cầu ví được gửi.
     */
    @Test
    void reviewRequestApproveDepositSuccessfully() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        User targetUser = createUser(targetUsername, "TargetNick", "pass123");
        double initialBalance = targetUser.getWallet().getBalance(); // = 0.0

        double depositAmount = 200.0;
        WalletRequest req = createWalletRequest(targetUsername, TransactionType.DEPOSIT, depositAmount);

        // Cả admin và target user đều online để kiểm tra sự kiện bắn về
        TestClientHandler adminClient = new TestClientHandler();
        adminClient.setCurrentUsername(adminUsername);
        RealtimeDatabase.addActiveUser(adminClient, adminUser);

        TestClientHandler targetClient = new TestClientHandler();
        targetClient.setCurrentUsername(targetUsername);
        RealtimeDatabase.addActiveUser(targetClient, targetUser);

        // Tạo request duyệt từ Admin
        WalletReviewRequest reviewData = new WalletReviewRequest(req.getId(), true); // true = Duyệt
        Request request = new Request(RequestType.REVIEW_WALLET_REQUEST, reviewData);

        Response response = adminWalletRequestService.reviewRequest(adminClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Request reviewed successfully", response.getMessage());

        // 1. Kiểm tra ví người dùng nhận tiền trong database SQLite
        User updatedUser = userDao.findByUsername(targetUsername);
        assertNotNull(updatedUser);
        assertEquals(initialBalance + depositAmount, updatedUser.getWallet().getBalance(), 0.001);

        // 2. Kiểm tra bản ghi giao dịch (Transaction) trong SQLite
        List<Transaction> txs = transactionDao.findByUsername(targetUsername);
        assertFalse(txs.isEmpty());
        Transaction latestTx = txs.get(0);
        assertEquals(TransactionType.DEPOSIT, latestTx.getType());
        assertEquals(depositAmount, latestTx.getAmount());

        // 3. Kiểm tra trạng thái yêu cầu ví trong SQLite
        WalletRequest updatedReq = walletRequestDao.findById(req.getId());
        assertNotNull(updatedReq);
        assertEquals(WalletRequestStatus.APPROVED, updatedReq.getStatus());
        assertEquals(adminUsername, updatedReq.getReviewedBy());

        // 4. Kiểm tra sự kiện gửi về cho Target User (WALLET_CHANGED)
        boolean hasWalletChangedEvent = targetClient.getSentEvents().stream()
                .anyMatch(e -> e.getType() == EventType.WALLET_CHANGED);
        assertTrue(hasWalletChangedEvent, "Target user phải nhận được sự kiện cập nhật ví.");
    }

    /**
     * Ca kiểm thử: Admin từ chối (Deny) yêu cầu nạp tiền (DEPOSIT) thành công.
     * Xác thực:
     * - Số dư ví của người dùng không đổi.
     * - Trạng thái yêu cầu ví chuyển thành DENIED.
     * - Không tạo ra giao dịch ví nào.
     */
    @Test
    void reviewRequestDenyDepositSuccessfully() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        User targetUser = createUser(targetUsername, "TargetNick", "pass123");
        double initialBalance = targetUser.getWallet().getBalance();

        WalletRequest req = createWalletRequest(targetUsername, TransactionType.DEPOSIT, 100.0);

        TestClientHandler adminClient = new TestClientHandler();
        adminClient.setCurrentUsername(adminUsername);
        RealtimeDatabase.addActiveUser(adminClient, adminUser);

        // Tạo request từ chối từ Admin
        WalletReviewRequest reviewData = new WalletReviewRequest(req.getId(), false); // false = Từ chối
        Request request = new Request(RequestType.REVIEW_WALLET_REQUEST, reviewData);

        Response response = adminWalletRequestService.reviewRequest(adminClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        // 1. Số dư ví người dùng không thay đổi trong SQLite
        User updatedUser = userDao.findByUsername(targetUsername);
        assertEquals(initialBalance, updatedUser.getWallet().getBalance(), 0.001);

        // 2. Không có giao dịch nào được tạo ra trong SQLite
        List<Transaction> txs = transactionDao.findByUsername(targetUsername);
        assertTrue(txs.isEmpty());

        // 3. Trạng thái yêu cầu ví chuyển thành DENIED trong SQLite
        WalletRequest updatedReq = walletRequestDao.findById(req.getId());
        assertEquals(WalletRequestStatus.DENIED, updatedReq.getStatus());
        assertEquals(adminUsername, updatedReq.getReviewedBy());
    }

    /**
     * Ca kiểm thử: Admin phê duyệt (Approve) yêu cầu rút tiền (WITHDRAW) thành công.
     * Khi rút tiền, số tiền bị giữ (lockedBalance) sẽ được khấu trừ khỏi tổng số dư.
     */
    @Test
    void reviewRequestApproveWithdrawSuccessfully() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        User targetUser = createUser(targetUsername, "TargetNick", "pass123");
        
        // Nạp sẵn 500$ cho user và khóa 200$ lại (tương ứng hành động xin rút 200$)
        targetUser.getWallet().deposit(500.0);
        targetUser.getWallet().lockBalance(200.0);
        userDao.save(targetUser, false);

        WalletRequest req = createWalletRequest(targetUsername, TransactionType.WITHDRAW, 200.0);

        TestClientHandler adminClient = new TestClientHandler();
        adminClient.setCurrentUsername(adminUsername);
        RealtimeDatabase.addActiveUser(adminClient, adminUser);

        TestClientHandler targetClient = new TestClientHandler();
        targetClient.setCurrentUsername(targetUsername);
        RealtimeDatabase.addActiveUser(targetClient, targetUser);

        // Phê duyệt yêu cầu rút tiền
        WalletReviewRequest reviewData = new WalletReviewRequest(req.getId(), true);
        Request request = new Request(RequestType.REVIEW_WALLET_REQUEST, reviewData);

        Response response = adminWalletRequestService.reviewRequest(adminClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        // 1. Xác thực số dư trong SQLite: khả dụng và đóng băng giảm đi
        User updatedUser = userDao.findByUsername(targetUsername);
        assertNotNull(updatedUser);
        // Balance ban đầu = 500$, rút 200$ còn 300$. lockedBalance giảm từ 200$ về 0$.
        assertEquals(300.0, updatedUser.getWallet().getBalance(), 0.001);
        assertEquals(0.0, updatedUser.getWallet().getLockedBalance(), 0.001);

        // 2. Kiểm tra bản ghi giao dịch rút tiền
        List<Transaction> txs = transactionDao.findByUsername(targetUsername);
        assertFalse(txs.isEmpty());
        assertEquals(TransactionType.WITHDRAW, txs.get(0).getType());

        // 3. Trạng thái yêu cầu ví thành APPROVED
        WalletRequest updatedReq = walletRequestDao.findById(req.getId());
        assertEquals(WalletRequestStatus.APPROVED, updatedReq.getStatus());
    }

    /**
     * Ca kiểm thử: Admin từ chối (Deny) yêu cầu rút tiền (WITHDRAW) thành công.
     * Số tiền đóng băng (lockedBalance) sẽ được mở khóa lại cho người dùng, tổng số dư vẫn giữ nguyên.
     */
    @Test
    void reviewRequestDenyWithdrawSuccessfully() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        User targetUser = createUser(targetUsername, "TargetNick", "pass123");

        // Nạp sẵn 500$ cho user và khóa 200$
        targetUser.getWallet().deposit(500.0);
        targetUser.getWallet().lockBalance(200.0);
        userDao.save(targetUser, false);

        WalletRequest req = createWalletRequest(targetUsername, TransactionType.WITHDRAW, 200.0);

        TestClientHandler adminClient = new TestClientHandler();
        adminClient.setCurrentUsername(adminUsername);
        RealtimeDatabase.addActiveUser(adminClient, adminUser);

        TestClientHandler targetClient = new TestClientHandler();
        targetClient.setCurrentUsername(targetUsername);
        RealtimeDatabase.addActiveUser(targetClient, targetUser);

        // Từ chối yêu cầu rút tiền
        WalletReviewRequest reviewData = new WalletReviewRequest(req.getId(), false);
        Request request = new Request(RequestType.REVIEW_WALLET_REQUEST, reviewData);

        Response response = adminWalletRequestService.reviewRequest(adminClient, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        // 1. Tổng số dư vẫn là 500$ nhưng lockedBalance giảm về 0$ (mở khóa)
        User updatedUser = userDao.findByUsername(targetUsername);
        assertNotNull(updatedUser);
        assertEquals(500.0, updatedUser.getWallet().getBalance(), 0.001);
        assertEquals(0.0, updatedUser.getWallet().getLockedBalance(), 0.001);

        // 2. Trạng thái yêu cầu ví thành DENIED
        WalletRequest updatedReq = walletRequestDao.findById(req.getId());
        assertEquals(WalletRequestStatus.DENIED, updatedReq.getStatus());
    }

    /**
     * Ca kiểm thử: Phê duyệt yêu cầu ví thất bại khi người gọi không phải Admin.
     */
    @Test
    void reviewRequestFailsForNonAdmin() {
        String username = uniqueUsername("user");
        User normalUser = createUser(username, "UserNick", "userPass");

        WalletRequest req = createWalletRequest(username, TransactionType.DEPOSIT, 100.0);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, normalUser);

        WalletReviewRequest reviewData = new WalletReviewRequest(req.getId(), true);
        Request request = new Request(RequestType.REVIEW_WALLET_REQUEST, reviewData);

        Response response = adminWalletRequestService.reviewRequest(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Admin permission required", response.getMessage());
    }

    /**
     * Ca kiểm thử: Phê duyệt thất bại khi yêu cầu ví không tồn tại trong SQLite.
     */
    @Test
    void reviewRequestFailsWhenRequestNotFound() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        TestClientHandler adminClient = new TestClientHandler();
        adminClient.setCurrentUsername(adminUsername);
        RealtimeDatabase.addActiveUser(adminClient, adminUser);

        // Sử dụng một ID ngẫu nhiên không có trong database
        WalletReviewRequest reviewData = new WalletReviewRequest(UUID.randomUUID().toString(), true);
        Request request = new Request(RequestType.REVIEW_WALLET_REQUEST, reviewData);

        Response response = adminWalletRequestService.reviewRequest(adminClient, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Wallet request not found", response.getMessage());
    }

    /**
     * Ca kiểm thử: Phê duyệt thất bại khi yêu cầu ví đã được duyệt/từ chối từ trước đó.
     */
    @Test
    void reviewRequestFailsWhenAlreadyReviewed() {
        String adminUsername = uniqueUsername("admin");
        User adminUser = createAdminUser(adminUsername, "AdminNick", "adminPass");

        String targetUsername = uniqueUsername("target");
        createUser(targetUsername, "TargetNick", "pass123");

        // Tạo yêu cầu ví
        WalletRequest req = createWalletRequest(targetUsername, TransactionType.DEPOSIT, 100.0);
        
        // Cập nhật trạng thái thành APPROVED trực tiếp
        req.approve(adminUsername);
        walletRequestDao.update(req);

        TestClientHandler adminClient = new TestClientHandler();
        adminClient.setCurrentUsername(adminUsername);
        RealtimeDatabase.addActiveUser(adminClient, adminUser);

        // Thực hiện cố phê duyệt tiếp yêu cầu đã APPROVED
        WalletReviewRequest reviewData = new WalletReviewRequest(req.getId(), true);
        Request request = new Request(RequestType.REVIEW_WALLET_REQUEST, reviewData);

        Response response = adminWalletRequestService.reviewRequest(adminClient, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Request already reviewed", response.getMessage());
    }

    /**
     * Hàm phụ trợ (Helper): Tạo một user bình thường và lưu vào database SQLite.
     */
    private User createUser(String username, String nickname, String rawPassword) {
        User user = new User(username, nickname, PasswordUtil.hash(rawPassword));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    /**
     * Hàm phụ trợ (Helper): Tạo một user Admin và lưu vào database SQLite.
     */
    private User createAdminUser(String username, String nickname, String rawPassword) {
        Admin admin = new Admin(username, nickname, PasswordUtil.hash(rawPassword));
        userDao.create(admin);
        createdUsernames.add(username);
        return admin;
    }

    /**
     * Hàm phụ trợ (Helper): Tạo một yêu cầu nạp/rút ví đang ở trạng thái PENDING và lưu vào SQLite.
     */
    private WalletRequest createWalletRequest(String username, TransactionType type, double amount) {
        WalletRequest request = new WalletRequest(username, type, amount);
        walletRequestDao.create(request);
        createdWalletRequestIds.add(request.getId());
        return request;
    }

    /**
     * Hàm phụ trợ (Helper): Tạo một username độc nhất để tránh trùng lặp dữ liệu test.
     */
    private String uniqueUsername(String prefix) {
        String normalizedPrefix = prefix.replace("-", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        int maxPrefixLength = Math.max(4, 20 - suffix.length());
        if (normalizedPrefix.length() > maxPrefixLength) {
            normalizedPrefix = normalizedPrefix.substring(0, maxPrefixLength);
        }
        return normalizedPrefix + suffix;
    }

    /**
     * Lớp giả lập (Mock/Stub) ClientHandler dùng riêng cho kiểm thử.
     * Giúp thu thập các sự kiện đã bắn về client và tránh phụ thuộc vào socket thật.
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

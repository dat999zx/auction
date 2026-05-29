package com.bidify.server.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.WalletRequestStatus;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.User;
import com.bidify.server.model.WalletRequest;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) cho {@link WalletRequestDao}.
 * Kiểm tra các tính năng:
 * - Tạo yêu cầu nạp/rút tiền mới và tìm lại theo ID.
 * - Cập nhật thông tin phê duyệt của yêu cầu nạp/rút (reviewedAt, status, reviewedBy).
 * - Truy vấn danh sách yêu cầu của một người dùng cụ thể.
 * - Lọc danh sách yêu cầu đang chờ duyệt (status = 'PENDING').
 * - Tính tổng số tiền rút đang chờ duyệt của một người dùng cụ thể.
 * Có chú thích chi tiết bằng tiếng Việt.
 */
class WalletRequestDaoTest {

    private final WalletRequestDao walletRequestDao = WalletRequestDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    private final List<String> createdUsernames = new ArrayList<>();
    private final List<String> createdRequestIds = new ArrayList<>();

    @BeforeAll
    static void initDatabase() {
        SQLiteHelper.init();
    }

    @BeforeEach
    void setUp() {
        createdUsernames.clear();
        createdRequestIds.clear();
    }

    @AfterEach
    void tearDown() {
        // Xóa các yêu cầu ví trước để đảm bảo ràng buộc khóa ngoại (FK)
        for (String reqId : createdRequestIds) {
            SQLiteHelper.update("DELETE FROM WalletRequests WHERE id = ?", reqId);
        }

        // Xóa người dùng thử nghiệm
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }
    }

    /**
     * Ca kiểm thử: Thêm mới yêu cầu ví thành công và tìm lại bằng ID.
     */
    @Test
    void createAndFindRequestSuccessfully() {
        String username = createTestUser("user");
        WalletRequest request = new WalletRequest(username, TransactionType.DEPOSIT, 500.0);

        walletRequestDao.create(request);
        createdRequestIds.add(request.getId());

        WalletRequest found = walletRequestDao.findById(request.getId());

        assertNotNull(found);
        assertEquals(request.getId(), found.getId());
        assertEquals(request.getUsername(), found.getUsername());
        assertEquals(TransactionType.DEPOSIT, found.getType());
        assertEquals(500.0, found.getAmount());
        assertEquals(WalletRequestStatus.PENDING, found.getStatus());
        assertNotNull(found.getCreatedAt());
        assertNull(found.getReviewedAt());
        assertNull(found.getReviewedBy());
    }

    /**
     * Ca kiểm thử: Cập nhật thông tin phê duyệt của yêu cầu ví thành công.
     */
    @Test
    void updateRequestSuccessfully() {
        String username = createTestUser("user");
        String admin = createTestUser("admin");
        WalletRequest request = new WalletRequest(username, TransactionType.WITHDRAW, 200.0);
        walletRequestDao.create(request);
        createdRequestIds.add(request.getId());

        // Thay đổi trạng thái thành APPROVED từ phía admin
        request.approve(admin);
        walletRequestDao.update(request);

        WalletRequest updated = walletRequestDao.findById(request.getId());
        assertNotNull(updated);
        assertEquals(WalletRequestStatus.APPROVED, updated.getStatus());
        assertEquals(admin, updated.getReviewedBy());
        assertNotNull(updated.getReviewedAt());
    }

    /**
     * Ca kiểm thử: Lấy danh sách yêu cầu ví của một người dùng, kiểm tra sắp xếp giảm dần theo thời gian tạo.
     */
    @Test
    void findByUsernameSuccessfully() {
        String username1 = createTestUser("user1");
        String username2 = createTestUser("user2");

        WalletRequest req1 = new WalletRequest(username1, TransactionType.DEPOSIT, 100.0);
        WalletRequest req2 = new WalletRequest(username1, TransactionType.WITHDRAW, 150.0);
        WalletRequest req3 = new WalletRequest(username2, TransactionType.DEPOSIT, 300.0);

        walletRequestDao.create(req1);
        walletRequestDao.create(req2);
        walletRequestDao.create(req3);
        createdRequestIds.add(req1.getId());
        createdRequestIds.add(req2.getId());
        createdRequestIds.add(req3.getId());

        List<WalletRequest> list = walletRequestDao.findByUsername(username1);

        assertNotNull(list);
        assertEquals(2, list.size());
        // Lịch sử trả về phải được sắp xếp theo thời gian mới nhất trước (req2 rồi tới req1)
        assertEquals(req2.getId(), list.get(0).getId());
        assertEquals(req1.getId(), list.get(1).getId());
    }

    /**
     * Ca kiểm thử: Lấy danh sách các yêu cầu đang ở trạng thái chờ duyệt (PENDING).
     */
    @Test
    void findPendingRequestsSuccessfully() {
        String username = createTestUser("user");
        String admin = createTestUser("admin");

        WalletRequest req1 = new WalletRequest(username, TransactionType.DEPOSIT, 100.0);
        WalletRequest req2 = new WalletRequest(username, TransactionType.WITHDRAW, 200.0);
        WalletRequest req3 = new WalletRequest(username, TransactionType.DEPOSIT, 300.0);

        walletRequestDao.create(req1);
        walletRequestDao.create(req2);
        walletRequestDao.create(req3);
        createdRequestIds.add(req1.getId());
        createdRequestIds.add(req2.getId());
        createdRequestIds.add(req3.getId());

        // Phê duyệt yêu cầu 2
        req2.approve(admin);
        walletRequestDao.update(req2);

        // Từ chối yêu cầu 3
        req3.deny(admin);
        walletRequestDao.update(req3);

        List<WalletRequest> pendingList = walletRequestDao.findPending();

        assertNotNull(pendingList);
        assertEquals(1, pendingList.size());
        assertEquals(req1.getId(), pendingList.get(0).getId());
        assertEquals(WalletRequestStatus.PENDING, pendingList.get(0).getStatus());
    }

    /**
     * Ca kiểm thử: Tính tổng số tiền rút đang chờ duyệt của người dùng.
     * Xác thực:
     * - Chỉ cộng các yêu cầu loại WITHDRAW và ở trạng thái PENDING.
     * - Bỏ qua các yêu cầu DEPOSIT hoặc yêu cầu đã được duyệt/từ chối.
     */
    @Test
    void sumPendingWithdrawsForUserSuccessfully() {
        String username = createTestUser("user");
        String admin = createTestUser("admin");

        // Yêu cầu rút 1: PENDING (được tính)
        WalletRequest withdrawPending1 = new WalletRequest(username, TransactionType.WITHDRAW, 100.0);
        // Yêu cầu rút 2: PENDING (được tính)
        WalletRequest withdrawPending2 = new WalletRequest(username, TransactionType.WITHDRAW, 250.0);
        // Yêu cầu rút 3: APPROVED (không được tính)
        WalletRequest withdrawApproved = new WalletRequest(username, TransactionType.WITHDRAW, 300.0);
        // Yêu cầu nạp: PENDING (không được tính)
        WalletRequest depositPending = new WalletRequest(username, TransactionType.DEPOSIT, 500.0);

        walletRequestDao.create(withdrawPending1);
        walletRequestDao.create(withdrawPending2);
        walletRequestDao.create(withdrawApproved);
        walletRequestDao.create(depositPending);

        createdRequestIds.add(withdrawPending1.getId());
        createdRequestIds.add(withdrawPending2.getId());
        createdRequestIds.add(withdrawApproved.getId());
        createdRequestIds.add(depositPending.getId());

        // Cập nhật trạng thái phê duyệt yêu cầu rút 3 thành APPROVED
        withdrawApproved.approve(admin);
        walletRequestDao.update(withdrawApproved);

        // Tính tổng tiền chờ rút
        double totalSum = walletRequestDao.sumPendingWithdrawsForUser(username);

        // 100.0 + 250.0 = 350.0
        assertEquals(350.0, totalSum, 0.001);
    }

    // --- Hàm trợ giúp (Helper Methods) ---

    private String createTestUser(String usernamePrefix) {
        String username = usernamePrefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        User user = new User(username, username, PasswordUtil.hash("pass123"));
        userDao.create(user);
        createdUsernames.add(username);
        return username;
    }
}

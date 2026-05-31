package com.bidify.server.model;

import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.WalletRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn giản cho WalletRequest.
 */
public class WalletRequestTest {

    @Test
    public void testWalletRequestInitializationWithDefaultConstructor() {
        String username = "user_wallet_req";
        double amount = 1000.0;

        // Khởi tạo yêu cầu nạp tiền mới
        WalletRequest req = new WalletRequest(username, TransactionType.DEPOSIT, amount);

        // Xác thực thuộc tính mặc định
        assertNotNull(req.getId(), "ID phải được tự động sinh ngẫu nhiên");
        assertNotNull(req.getCreatedAt(), "Thời điểm tạo không được null");
        assertNull(req.getReviewedAt(), "ReviewedAt ban đầu phải là null");
        assertNull(req.getReviewedBy(), "ReviewedBy ban đầu phải là null");
        assertEquals(username, req.getUsername());
        assertEquals(TransactionType.DEPOSIT, req.getType());
        assertEquals(amount, req.getAmount());
        assertEquals(WalletRequestStatus.PENDING, req.getStatus(), "Trạng thái ban đầu phải là PENDING");
    }

    @Test
    public void testWalletRequestInitializationWithDatabaseConstructor() {
        String id = "req-custom-id";
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 29, 3, 0, 0);
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 5, 29, 3, 10, 0);
        String username = "user_db";
        TransactionType type = TransactionType.WITHDRAW;
        double amount = 200.0;
        WalletRequestStatus status = WalletRequestStatus.APPROVED;
        String reviewer = "admin_user";

        // Constructor nạp từ database
        WalletRequest req = new WalletRequest(id, createdAt, reviewedAt, username, type, amount, status, reviewer);

        // Xác thực bảo toàn thuộc tính
        assertEquals(id, req.getId());
        assertEquals(createdAt, req.getCreatedAt());
        assertEquals(reviewedAt, req.getReviewedAt());
        assertEquals(username, req.getUsername());
        assertEquals(type, req.getType());
        assertEquals(amount, req.getAmount());
        assertEquals(status, req.getStatus());
        assertEquals(reviewer, req.getReviewedBy());
    }

    @Test
    public void testWalletRequestApproval() {
        WalletRequest req = new WalletRequest("user_test", TransactionType.DEPOSIT, 500.0);
        assertNull(req.getReviewedAt());
        assertNull(req.getReviewedBy());

        // Duyệt yêu cầu
        req.approve("admin_approver");

        // Xác thực trạng thái
        assertEquals(WalletRequestStatus.APPROVED, req.getStatus(), "Trạng thái phải chuyển sang APPROVED");
        assertNotNull(req.getReviewedAt(), "Thời điểm duyệt phải được ghi nhận");
        assertEquals("admin_approver", req.getReviewedBy(), "Người duyệt phải khớp");
    }

    @Test
    public void testWalletRequestDenial() {
        WalletRequest req = new WalletRequest("user_test", TransactionType.WITHDRAW, 300.0);
        assertNull(req.getReviewedAt());
        assertNull(req.getReviewedBy());

        // Từ chối yêu cầu
        req.deny("admin_denier");

        // Xác thực trạng thái
        assertEquals(WalletRequestStatus.DENIED, req.getStatus(), "Trạng thái phải chuyển sang DENIED");
        assertNotNull(req.getReviewedAt(), "Thời điểm từ chối phải được ghi nhận");
        assertEquals("admin_denier", req.getReviewedBy(), "Người từ chối phải khớp");
    }
}

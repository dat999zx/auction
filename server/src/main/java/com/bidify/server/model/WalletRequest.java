package com.bidify.server.model;

import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.WalletRequestStatus;

import java.time.LocalDateTime;

import com.bidify.common.utility.TimeUtil;
import java.util.UUID;

// Yêu cầu nạp/rút tiền của user — chờ admin duyệt mới được thực hiện
public class WalletRequest {
    // ID định danh yêu cầu
    private String id;
    // Thời điểm gửi yêu cầu
    private LocalDateTime createdAt;
    // Thời điểm admin duyệt/từ chối (null nếu chưa duyệt)
    private LocalDateTime reviewedAt;
    // Username người gửi yêu cầu
    private String username;
    // Loại: DEPOSIT (nạp) hoặc WITHDRAW (rút)
    private TransactionType type;
    // Số tiền muốn nạp/rút
    private double amount;
    // Trạng thái: PENDING, APPROVED, DENIED
    private WalletRequestStatus status;
    // Username admin đã duyệt (null nếu chưa duyệt)
    private String reviewedBy;

    // Khởi tạo yêu cầu nạp/rút mới với ID tự sinh và thời gian hiện tại.
    public WalletRequest(String username, TransactionType type, double amount) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = TimeUtil.nowInVietnam();
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.status = WalletRequestStatus.PENDING;
    }

    public WalletRequest(String id, LocalDateTime createdAt, LocalDateTime reviewedAt, String username, TransactionType type, double amount, WalletRequestStatus status, String reviewedBy) {
        this.id = id;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.reviewedBy = reviewedBy;
    }

    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getUsername() { return username; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public WalletRequestStatus getStatus() { return status; }
    public String getReviewedBy() { return reviewedBy; }

    // Admin duyệt yêu cầu — chuyển status sang APPROVED và ghi lại ai duyệt lúc nào
    public void approve(String reviewer) {
        this.status = WalletRequestStatus.APPROVED;
        this.reviewedAt = TimeUtil.nowInVietnam();
        this.reviewedBy = reviewer;
    }

    // Admin từ chối yêu cầu — chuyển status sang DENIED và ghi lại ai từ chối lúc nào
    public void deny(String reviewer) {
        this.status = WalletRequestStatus.DENIED;
        this.reviewedAt = TimeUtil.nowInVietnam();
        this.reviewedBy = reviewer;
    }
}

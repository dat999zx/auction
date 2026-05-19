package com.bidify.server.model;

import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.WalletRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class WalletRequest {
    private String id;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private String username;
    private TransactionType type;
    private double amount;
    private WalletRequestStatus status;
    private String reviewedBy;

    // dùng để tạo một yêu cầu ví mới với ID ngẫu nhiên và thời gian hiện tại
    public WalletRequest(String username, TransactionType type, double amount) {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.status = WalletRequestStatus.PENDING;
    }

    // dùng để tải thông tin yêu cầu ví từ database khi đã có sẵn đầy đủ thuộc tính
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

    // dùng để lấy ID của yêu cầu
    public String getId() { return id; }
    // dùng để lấy ngày tạo yêu cầu
    public LocalDateTime getCreatedAt() { return createdAt; }
    // dùng để lấy ngày duyệt yêu cầu
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    // dùng để lấy tên người dùng gửi yêu cầu
    public String getUsername() { return username; }
    // dùng để lấy loại giao dịch (nạp/rút)
    public TransactionType getType() { return type; }
    // dùng để lấy số tiền giao dịch
    public double getAmount() { return amount; }
    // dùng để lấy trạng thái duyệt của yêu cầu
    public WalletRequestStatus getStatus() { return status; }
    // dùng để lấy tên người duyệt yêu cầu
    public String getReviewedBy() { return reviewedBy; }

    // dùng để phê duyệt yêu cầu nạp/rút tiền này
    public void approve(String reviewer) {
        this.status = WalletRequestStatus.APPROVED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewer;
    }

    // dùng để từ chối yêu cầu nạp/rút tiền này
    public void deny(String reviewer) {
        this.status = WalletRequestStatus.DENIED;
        this.reviewedAt = LocalDateTime.now();
        this.reviewedBy = reviewer;
    }
}

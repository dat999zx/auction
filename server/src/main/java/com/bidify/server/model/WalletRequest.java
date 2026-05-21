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

    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public String getUsername() { return username; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public WalletRequestStatus getStatus() { return status; }
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

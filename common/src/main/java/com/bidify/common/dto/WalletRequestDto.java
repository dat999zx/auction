package com.bidify.common.dto;

import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.WalletRequestStatus;

public class WalletRequestDto {
    private String id;
    private String createdAt;
    private String reviewedAt;
    private String username;
    private TransactionType type;
    private double amount;
    private WalletRequestStatus status;
    private String reviewedBy;

    public WalletRequestDto() {}

    public WalletRequestDto(String id, String createdAt, String reviewedAt, String username, TransactionType type, double amount, WalletRequestStatus status, String reviewedBy) {
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
    public String getCreatedAt() { return createdAt; }
    public String getReviewedAt() { return reviewedAt; }
    public String getUsername() { return username; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public WalletRequestStatus getStatus() { return status; }
    public String getReviewedBy() { return reviewedBy; }
}

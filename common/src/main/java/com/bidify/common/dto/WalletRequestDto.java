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

    // dùng để tạo một đối tượng DTO trống
    public WalletRequestDto() {}

    // dùng để tạo một đối tượng DTO đầy đủ thông tin yêu cầu ví
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

    // dùng để lấy ID của yêu cầu
    public String getId() { return id; }
    // dùng để lấy thời gian tạo yêu cầu
    public String getCreatedAt() { return createdAt; }
    // dùng để lấy thời gian duyệt yêu cầu
    public String getReviewedAt() { return reviewedAt; }
    // dùng để lấy tên người dùng gửi yêu cầu
    public String getUsername() { return username; }
    // dùng để lấy loại giao dịch (nạp hoặc rút)
    public TransactionType getType() { return type; }
    // dùng để lấy số tiền giao dịch
    public double getAmount() { return amount; }
    // dùng để lấy trạng thái duyệt của yêu cầu
    public WalletRequestStatus getStatus() { return status; }
    // dùng để lấy tên admin đã duyệt yêu cầu
    public String getReviewedBy() { return reviewedBy; }
}

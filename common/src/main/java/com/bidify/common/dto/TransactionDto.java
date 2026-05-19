package com.bidify.common.dto;

import com.bidify.common.enums.TransactionType;

public class TransactionDto {
    private String id;
    private String createdAt;
    private String username;
    private TransactionType type;
    private double amount;
    private String auctionId;

    // dùng để tạo một đối tượng TransactionDto
    public TransactionDto(String id, String createdAt, String username, TransactionType type, double amount, String auctionId) {
        this.id = id;
        this.createdAt = createdAt;
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
    }

    // dùng để lấy ID
    public String getId() { return id; }
    // dùng để lấy created tại
    public String getCreatedAt() { return createdAt; }
    // dùng để lấy username
    public String getUsername() { return username; }
    // dùng để lấy type
    public TransactionType getType() { return type; }
    // dùng để lấy số tiền
    public double getAmount() { return amount; }
    // dùng để lấy đấu giá ID
    public String getAuctionId() { return auctionId; }
}

package com.bidify.common.dto;

import com.bidify.common.enums.TransactionType;

public class TransactionDto {
    private String id;
    private String createdAt;
    private String username;
    private TransactionType type;
    private double amount;
    private String auctionId;

    public TransactionDto(String id, String createdAt, String username, TransactionType type, double amount, String auctionId) {
        this.id = id;
        this.createdAt = createdAt;
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
    }

    public String getId() { return id; }
    public String getCreatedAt() { return createdAt; }
    public String getUsername() { return username; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public String getAuctionId() { return auctionId; }
}

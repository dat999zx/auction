package com.bidify.common.dto;

public class BidDto {
    private String id;
    private String createdAt;
    private double amount;
    private String bidderUsername;
    private String auctionId;

    public BidDto() {}

    public BidDto(String id, String createdAt, String auctionId, String bidderUsername, double amount) {
        this.id = id;
        this.createdAt = createdAt;
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
    }

    public String getId() {
        return id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public double getAmount() {
        return amount;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public String getAuctionId() {
        return auctionId;
    }
}

package com.bidify.common.dto;

public class BidDto {
    private String id;
    private String createdAt;
    private double amount;
    private String bidderUsername;
    private String auctionId;
    private boolean autoBidGenerated;

    public BidDto(String id, String createdAt, String auctionId, String bidderUsername, double amount) {
        this(id, createdAt, auctionId, bidderUsername, amount, false);
    }

    public BidDto(String id, String createdAt, String auctionId, String bidderUsername, double amount, boolean autoBidGenerated) {
        this.id = id;
        this.createdAt = createdAt;
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.autoBidGenerated = autoBidGenerated;
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

    public boolean isAutoBidGenerated() {
        return autoBidGenerated;
    }
}

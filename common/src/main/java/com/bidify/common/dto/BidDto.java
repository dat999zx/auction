package com.bidify.common.dto;

public class BidDto {
    private String id;
    private double amount;
    private String bidderUsername;
    private String auctionId;

    public BidDto() {}

    public BidDto(String id, double amount, String bidderUsername, String auctionId) {
        this.id = id;
        this.amount = amount;
        this.bidderUsername = bidderUsername;
        this.auctionId = auctionId;
    }

    public String getId() {
        return id;
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

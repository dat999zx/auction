package com.bidify.server.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private String auctionId;
    private String bidderId;
    private String itemId;
    private double bidAmount;
    private LocalDateTime bidTime;
    private String bidStatus;

    public BidTransaction() {
        super();
        this.bidTime = LocalDateTime.now();
        this.bidStatus = "ACTIVE";
    }

    public BidTransaction(String auctionId, String bidderId, String itemId, double bidAmount) {
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.itemId = itemId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
        this.bidStatus = "ACTIVE";
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public String getBidStatus() {
        return bidStatus;
    }

    public void setBidStatus(String bidStatus) {
        this.bidStatus = bidStatus;
    }
}


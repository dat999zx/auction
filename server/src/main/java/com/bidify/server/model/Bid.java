package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.IdGenerator;

public class Bid extends Entity {
    private String auctionId, bidderUsername;
    private double amount;

    public Bid(String auctionId, String bidderUsername, double amount){
        super(IdGenerator.genBidId(), LocalDateTime.now());
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
    }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public String getBidderUsername() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}

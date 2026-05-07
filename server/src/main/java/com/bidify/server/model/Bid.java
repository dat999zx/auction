package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.IdGenerator;

public class Bid extends Entity {
    private String auctionId, bidderUsername;
    private double amount;

    // tạo Bid mới
    public Bid(String auctionId, String bidderUsername, double amount){
        super(IdGenerator.genBidId(), LocalDateTime.now());
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
    }

    // load từ db
    public Bid(String id, LocalDateTime createdAt, String auctionId, String bidderUsername, double amount) {
        super(id, createdAt);
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
    } 

    public String getAuctionId() { return auctionId; }
    public String getBidderUsername() { return bidderUsername; }
    public double getAmount() { return amount; }
}


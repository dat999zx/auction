package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.IdGenerator;

public class Bid extends Entity {
    private String auctionId;
    private String bidderUsername;
    private double amount;
    private boolean autoBidGenerated;

    public Bid(String auctionId, String bidderUsername, double amount) {
        this(auctionId, bidderUsername, amount, false);
    }

    public Bid(String auctionId, String bidderUsername, double amount, boolean autoBidGenerated) {
        super(IdGenerator.genBidId(), LocalDateTime.now());
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.autoBidGenerated = autoBidGenerated;
    }

    public Bid(String id, LocalDateTime createdAt, String auctionId, String bidderUsername, double amount) {
        this(id, createdAt, auctionId, bidderUsername, amount, false);
    }

    public Bid(String id, LocalDateTime createdAt, String auctionId, String bidderUsername, double amount, boolean autoBidGenerated) {
        super(id, createdAt);
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.autoBidGenerated = autoBidGenerated;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isAutoBidGenerated() {
        return autoBidGenerated;
    }
}

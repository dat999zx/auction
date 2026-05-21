package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.IdGenerator;

public class Bid extends Entity {
    private String auctionId;
    private String bidderUsername;
    private double amount;
    private boolean autoBidGenerated;

    // dùng để tạo một đối tượng Bid
    public Bid(String auctionId, String bidderUsername, double amount) {
        // dùng để this
        this(auctionId, bidderUsername, amount, false);
    }

    // dùng để tạo một đối tượng Bid
    public Bid(String auctionId, String bidderUsername, double amount, boolean autoBidGenerated) {
        super(IdGenerator.genBidId(), LocalDateTime.now());
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.autoBidGenerated = autoBidGenerated;
    }

    // dùng để tạo một đối tượng Bid
    public Bid(String id, LocalDateTime createdAt, String auctionId, String bidderUsername, double amount) {
        // dùng để this
        this(id, createdAt, auctionId, bidderUsername, amount, false);
    }

    // dùng để tạo một đối tượng Bid
    public Bid(String id, LocalDateTime createdAt, String auctionId, String bidderUsername, double amount, boolean autoBidGenerated) {
        // dùng để super
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

    // dùng để kiểm tra xem auto lượt đặt giá generated
    public boolean isAutoBidGenerated() {
        return autoBidGenerated;
    }
}

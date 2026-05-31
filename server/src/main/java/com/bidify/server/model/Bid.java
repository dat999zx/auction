package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.IdGenerator;
import com.bidify.common.utility.TimeUtil;

// Một lần đặt giá trong phiên đấu giá
public class Bid extends Entity {
    // ID phiên đấu giá mà bid này thuộc về
    private String auctionId;
    // Username người đặt giá
    private String bidderUsername;
    // Số tiền đặt
    private double amount;
    // true nếu bid này do hệ thống tự đặt (auto bid), false nếu user tự bấm
    private boolean autoBidGenerated;

    public Bid(String auctionId, String bidderUsername, double amount) {
        this(auctionId, bidderUsername, amount, false);
    }

    public Bid(String auctionId, String bidderUsername, double amount, boolean autoBidGenerated) {
        super(IdGenerator.genBidId(), TimeUtil.nowInVietnam());
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

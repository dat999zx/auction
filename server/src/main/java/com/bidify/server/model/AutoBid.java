package com.bidify.server.model;

import java.time.LocalDateTime;

public class AutoBid {
    private final String auctionId;
    private final String username;
    private final LocalDateTime createdAt;
    private double maxBid;
    private boolean enabled;

    public AutoBid(String auctionId, String username, double maxBid) {
        this.auctionId = auctionId;
        this.username = username;
        this.maxBid = maxBid;
        this.createdAt = LocalDateTime.now();
        this.enabled = true;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getUsername() {
        return username;
    }

    public double getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(double maxBid) {
        this.maxBid = maxBid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void disable() {
        this.enabled = false;
    }
}

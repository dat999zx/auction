package com.bidify.server.model;

import java.time.LocalDateTime;

public class AutoBid {
    private final String auctionId;
    private final String username;
    private final LocalDateTime createdAt;
    private double maxBid;
    private boolean enabled;

    // dùng để tạo một đối tượng AutoBid
    public AutoBid(String auctionId, String username, double maxBid) {
        this.auctionId = auctionId;
        this.username = username;
        this.maxBid = maxBid;
        this.createdAt = LocalDateTime.now();
        this.enabled = true;
    }

    // dùng để lấy đấu giá ID
    public String getAuctionId() {
        return auctionId;
    }

    // dùng để lấy username
    public String getUsername() {
        return username;
    }

    // dùng để lấy max lượt đặt giá
    public double getMaxBid() {
        return maxBid;
    }

    // dùng để thiết lập max lượt đặt giá
    public void setMaxBid(double maxBid) {
        this.maxBid = maxBid;
    }

    // dùng để lấy created tại
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // dùng để kiểm tra xem enabled
    public boolean isEnabled() {
        return enabled;
    }

    // dùng để disable
    public void disable() {
        this.enabled = false;
    }
}

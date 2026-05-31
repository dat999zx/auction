package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.TimeUtil;

// Cấu hình đặt giá tự động của 1 user trong 1 phiên — server tự đặt giá thay user khi bị vượt
public class AutoBid {
    // ID phiên đấu giá đang áp dụng auto bid
    private final String auctionId;
    // Username người cài auto bid
    private final String username;
    // Thời điểm cài auto bid
    private final LocalDateTime createdAt;
    // Giá tối đa sẵn sàng trả — server sẽ không tự vượt mức này
    private double maxBid;
    // true = đang hoạt động, false = đã tắt
    private boolean enabled;

    public AutoBid(String auctionId, String username, double maxBid) {
        this.auctionId = auctionId;
        this.username = username;
        this.maxBid = maxBid;
        this.createdAt = TimeUtil.nowInVietnam();
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

    // Tắt auto bid — hệ thống sẽ không tự đặt giá thay user này nữa
    public void disable() {
        this.enabled = false;
    }
}

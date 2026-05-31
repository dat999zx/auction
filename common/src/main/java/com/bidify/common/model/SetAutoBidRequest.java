package com.bidify.common.model;

// Bật chế độ đặt giá tự động — server tự tăng giá thay user đến khi đạt maxBid
public class SetAutoBidRequest {
    // ID phiên đấu giá muốn bật auto bid
    private String auctionId;
    // Giá tối đa sẵn sàng trả — hệ thống tự đặt giá đến khi chạm giới hạn này
    private double maxBid;

    public SetAutoBidRequest(String auctionId, double maxBid) {
        this.auctionId = auctionId;
        this.maxBid = maxBid;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getMaxBid() {
        return maxBid;
    }
}

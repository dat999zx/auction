package com.bidify.common.model;

// Yêu cầu tham gia (subscribe) vào kênh realtime của 1 phiên đấu giá
public class JoinAuctionRequest {
    // ID phiên đấu giá muốn vào xem
    private String auctionId;

    public JoinAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() { return auctionId; }
}

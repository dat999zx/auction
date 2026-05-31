package com.bidify.common.model;

// Yêu cầu rời (unsubscribe) khỏi kênh realtime của 1 phiên đấu giá
public class LeaveAuctionRequest {
    // ID phiên đấu giá muốn rời
    private String auctionId;

    public LeaveAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }
    
    public String getAuctionId() { return auctionId; }
}

package com.bidify.common.model;

public class LeaveAuctionRequest {
    private String auctionId;

    // dùng để tạo một đối tượng LeaveAuctionRequest
    public LeaveAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }
    
    // dùng để lấy đấu giá ID
    public String getAuctionId() { return auctionId; }
}

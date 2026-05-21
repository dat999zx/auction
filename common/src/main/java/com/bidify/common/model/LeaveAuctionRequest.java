package com.bidify.common.model;

public class LeaveAuctionRequest {
    private String auctionId;

    // dùng để tạo một đối tượng LeaveAuctionRequest
    public LeaveAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }
    
    public String getAuctionId() { return auctionId; }
}

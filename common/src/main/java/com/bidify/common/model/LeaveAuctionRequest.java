package com.bidify.common.model;

public class LeaveAuctionRequest {
    private String auctionId;

    public LeaveAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }
    
    public String getAuctionId() { return auctionId; }
}

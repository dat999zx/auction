package com.bidify.common.model;

public class SetAutoBidRequest {
    private String auctionId;
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

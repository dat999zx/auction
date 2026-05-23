package com.bidify.common.model;

public class DisableAutoBidRequest {
    private String auctionId;

    public DisableAutoBidRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}

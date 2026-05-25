package com.bidify.common.model;

public class PayAuctionRequest {
    private String auctionId;

    public PayAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}

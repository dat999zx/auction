package com.bidify.common.model;

public class JoinAuctionRequest {
    private String auctionId;

    public JoinAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() { return auctionId; }
}

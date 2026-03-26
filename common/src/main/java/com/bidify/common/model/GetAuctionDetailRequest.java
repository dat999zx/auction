package com.bidify.common.model;

public class GetAuctionDetailRequest {
    private String auctionId;

    public GetAuctionDetailRequest() {}
    public GetAuctionDetailRequest(String auctionId) { this.auctionId = auctionId; }

    public String getAuctionId() { return auctionId; }
}

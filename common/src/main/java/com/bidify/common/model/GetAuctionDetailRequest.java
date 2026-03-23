package com.bidify.common.model;

public class GetAuctionDetailRequest {
    private String seller;

    public GetAuctionDetailRequest() {}
    public GetAuctionDetailRequest(String seller) { this.seller = seller; }

    public String getSeller() { return seller; }
}

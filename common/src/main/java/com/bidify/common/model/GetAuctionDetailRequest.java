package com.bidify.common.model;

public class GetAuctionDetailRequest {
    private String auctionId;

    // dùng để tạo một đối tượng GetAuctionDetailRequest
    public GetAuctionDetailRequest() {}
    // dùng để tạo một đối tượng GetAuctionDetailRequest
    public GetAuctionDetailRequest(String auctionId) { this.auctionId = auctionId; }

    // dùng để lấy đấu giá ID
    public String getAuctionId() { return auctionId; }
}

package com.bidify.common.model;

public class JoinAuctionRequest {
    private String auctionId;

    // dùng để tạo một đối tượng JoinAuctionRequest
    public JoinAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    // dùng để lấy đấu giá ID
    public String getAuctionId() { return auctionId; }
}

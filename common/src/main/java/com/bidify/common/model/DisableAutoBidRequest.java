package com.bidify.common.model;

public class DisableAutoBidRequest {
    private String auctionId;

    // dùng để tạo một đối tượng DisableAutoBidRequest
    public DisableAutoBidRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}

package com.bidify.common.model;

// Tắt chế độ đặt giá tự động cho 1 phiên đấu giá
public class DisableAutoBidRequest {
    // ID phiên đấu giá muốn tắt auto bid
    private String auctionId;

    public DisableAutoBidRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}

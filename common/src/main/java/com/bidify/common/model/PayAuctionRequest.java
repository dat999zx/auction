package com.bidify.common.model;

// Người thắng gửi yêu cầu thanh toán tiền cho phiên đấu giá đã thắng
public class PayAuctionRequest {
    // ID phiên đấu giá cần thanh toán
    private String auctionId;

    public PayAuctionRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}

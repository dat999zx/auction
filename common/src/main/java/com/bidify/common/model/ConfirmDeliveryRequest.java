package com.bidify.common.model;

// Người mua xác nhận đã nhận được hàng — hoàn tất giao dịch
public class ConfirmDeliveryRequest {
    // ID phiên đấu giá cần xác nhận đã nhận hàng
    private String auctionId;

    public ConfirmDeliveryRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}

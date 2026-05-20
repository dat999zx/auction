package com.bidify.common.model;

public class ConfirmDeliveryRequest {
    private String auctionId;

    public ConfirmDeliveryRequest() {}

    public ConfirmDeliveryRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }
}

package com.bidify.common.model;

public class PlaceBidRequest {
    private String auctionId;
    private double bidAmount;

    // dùng để tạo một đối tượng PlaceBidRequest
    public PlaceBidRequest(String auctionId, double bidAmount){
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }

    public String getAuctionId(){ return auctionId; }
    public double getBidAmount(){ return bidAmount; }
}

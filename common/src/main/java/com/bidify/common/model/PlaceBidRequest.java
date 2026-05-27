package com.bidify.common.model;

// Dữ liệu gửi lên khi đặt giá vào 1 phiên đấu giá
public class PlaceBidRequest {
    // ID phiên đấu giá muốn đặt giá
    private String auctionId;
    // Số tiền muốn đặt
    private double bidAmount;

    public PlaceBidRequest(String auctionId, double bidAmount){
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }

    public String getAuctionId(){ return auctionId; }
    public double getBidAmount(){ return bidAmount; }
}

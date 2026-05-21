package com.bidify.common.model;

public class CreateAuctionRequest {
    private String seller;
    private String itemId;
    private double startingPrice, minIncrement;
    private String startTime, endTime;

    public CreateAuctionRequest(
        String seller,
        String itemId,
        double startingPrice,
        double minIncrement,
        String startTime,
        String endTime
    ) {
        this.seller = seller;
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getSeller(){ return seller; }

    public String getItemId() { return itemId; }

    public double getStartingPrice() { return startingPrice; }

    public double getMinIncrement() { return minIncrement; }

    public String getEndTime() { return endTime; }

    public String getStartTime() { return startTime; }
}

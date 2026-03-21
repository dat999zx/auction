package com.bidify.common.model;

public class CreateAuctionRequest {
    private String seller;
    private String auctionName, description;
    private double startingPrice;
    private String startTime, endTime;
    public CreateAuctionRequest(String seller, String name, String description, double startingPrice, String startTime, String endTime){
        this.seller = seller;
        this.auctionName = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public String getAuctionName() { return auctionName; }

    public String getDescription() { return description; }

    public double getStartingPrice() { return startingPrice; }
    
    public String getEndTime() { return endTime; }

    public String getStartTime() { return startTime; }
    
    public String getSeller(){ return seller; }
}

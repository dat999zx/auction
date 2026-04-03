package com.bidify.common.model;

public class CreateAuctionRequest {
    private String seller;
    private String auctionName, description, category, productType;
    private double startingPrice, minIncrement;
    private String startTime, endTime;

    public CreateAuctionRequest(
        String seller,
        String name,
        String description,
        String category,
        String productType,
        double startingPrice,
        double minIncrement,
        String startTime,
        String endTime
    ) {
        this.seller = seller;
        this.auctionName = name;
        this.description = description;
        this.category = category;
        this.productType = productType;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getAuctionName() { return auctionName; }

    public String getDescription() { return description; }

    public String getCategory() { return category; }

    public String getProductType() { return productType; }

    public double getStartingPrice() { return startingPrice; }

    public double getMinIncrement() { return minIncrement; }

    public String getEndTime() { return endTime; }

    public String getStartTime() { return startTime; }

    public String getSeller(){ return seller; }
}

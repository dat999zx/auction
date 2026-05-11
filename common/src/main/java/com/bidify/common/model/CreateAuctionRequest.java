package com.bidify.common.model;

import java.util.List;

public class CreateAuctionRequest {
    private String seller;
    private String auctionName, description, category, productType;
    private double startingPrice, minIncrement;
    private String startTime, endTime;
    private List<String> imagesBase64;

    public CreateAuctionRequest(
        String seller,
        String name,
        String description,
        String category,
        String productType,
        double startingPrice,
        double minIncrement,
        String startTime,
        String endTime,
        List<String> imagesBase64
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
        this.imagesBase64 = imagesBase64;
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

    public List<String> getImagesBase64() { return imagesBase64; }
}

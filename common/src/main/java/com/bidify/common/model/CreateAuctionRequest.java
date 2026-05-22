package com.bidify.common.model;

public class CreateAuctionRequest {
    private String seller;
    private String itemId;
    private double startingPrice, minIncrement;
    private String startTime, endTime;
    private String triggerTime; // minTime, HH:MM
    private String extensionTime; // HH:MM
    private String maxExtensionTime; // HH:MM

    public CreateAuctionRequest(
        String seller,
        String itemId,
        double startingPrice,
        double minIncrement,
        String startTime,
        String endTime,
        String triggerTime,
        String extensionTime,
        String maxExtensionTime
    ) {
        this.seller = seller;
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.triggerTime = triggerTime;
        this.extensionTime = extensionTime;
        this.maxExtensionTime = maxExtensionTime;
    }

    public String getSeller(){ return seller; }

    public String getItemId() { return itemId; }

    public double getStartingPrice() { return startingPrice; }

    public double getMinIncrement() { return minIncrement; }

    public String getEndTime() { return endTime; }

    public String getStartTime() { return startTime; }

    public String getTriggerTime() { return triggerTime; }

    public String getExtensionTime() { return extensionTime; }

    public String getMaxExtensionTime() { return maxExtensionTime; }
}

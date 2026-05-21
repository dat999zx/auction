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

    // dùng để lấy seller
    public String getSeller(){ return seller; }

    // dùng để lấy sản phẩm ID
    public String getItemId() { return itemId; }

    // dùng để lấy starting giá sản phẩm
    public double getStartingPrice() { return startingPrice; }

    // dùng để lấy min increment
    public double getMinIncrement() { return minIncrement; }

    // dùng để lấy end thời gian
    public String getEndTime() { return endTime; }

    // dùng để lấy bắt đầu thời gian
    public String getStartTime() { return startTime; }

    public String getTriggerTime() { return triggerTime; }

    public String getExtensionTime() { return extensionTime; }

    public String getMaxExtensionTime() { return maxExtensionTime; }
}

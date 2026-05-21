package com.bidify.common.model;

public class UpdateAuctionRequest {
    private String auctionId;
    private String auctionName, description;
    private double startingPrice, minIncrement;
    private String startTime, endTime;
    private String message; // tin nhắn gửi cho người tham gia
    private String triggerTime; // HH:MM
    private String extensionTime;
    private String maxExtensionTime;

    public UpdateAuctionRequest(String auctionId, String auctionName, String description, double startingPrice, double minIncrement,
            String startTime, String endTime, String message, String triggerTime, String extensionTime, String maxExtensionTime) {
        this.auctionId = auctionId;
        this.auctionName = auctionName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.message = message;
        this.triggerTime = triggerTime;
        this.extensionTime = extensionTime;
        this.maxExtensionTime = maxExtensionTime;
    }

    public String getAuctionId() { return auctionId; }

    public String getAuctionName() { return auctionName; }

    public String getDescription() { return description; }

    public double getStartingPrice() { return startingPrice; }

    public double getMinIncrement() { return minIncrement; }

    public String getStartTime() { return startTime; }

    public String getEndTime() { return endTime; }

    public String getMessage() { return message; }

    public String getTriggerTime() { return triggerTime; }

    public String getExtensionTime() { return extensionTime; }

    public String getMaxExtensionTime() { return maxExtensionTime; }
}

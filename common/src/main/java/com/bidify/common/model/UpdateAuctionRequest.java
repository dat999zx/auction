package com.bidify.common.model;

public class UpdateAuctionRequest {
    private String auctionId;
    private String auctionName, description;
    private double startingPrice, minIncrement;
    private String startTime, endTime;
    private String message; // tin nhắn gửi cho người tham gia

    public UpdateAuctionRequest(String auctionId, String auctionName, String description, double startingPrice, double minIncrement,
            String startTime, String endTime, String message) {
        this.auctionId = auctionId;
        this.auctionName = auctionName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.message = message;
    }

    public String getAuctionId() { return auctionId; }

    public String getAuctionName() { return auctionName; }

    public String getDescription() { return description; }

    public double getStartingPrice() { return startingPrice; }

    public double getMinIncrement() { return minIncrement; }

    public String getStartTime() { return startTime; }

    public String getEndTime() { return endTime; }

    public String getMessage() { return message; }
}

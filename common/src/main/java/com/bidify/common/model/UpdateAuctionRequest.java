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

    // dùng để lấy đấu giá ID
    public String getAuctionId() { return auctionId; }

    // dùng để lấy đấu giá tên
    public String getAuctionName() { return auctionName; }

    // dùng để lấy description
    public String getDescription() { return description; }

    // dùng để lấy starting giá sản phẩm
    public double getStartingPrice() { return startingPrice; }

    // dùng để lấy min increment
    public double getMinIncrement() { return minIncrement; }

    // dùng để lấy bắt đầu thời gian
    public String getStartTime() { return startTime; }

    // dùng để lấy end thời gian
    public String getEndTime() { return endTime; }

    // dùng để lấy tin nhắn
    public String getMessage() { return message; }
}

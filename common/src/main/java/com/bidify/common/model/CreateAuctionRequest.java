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
}

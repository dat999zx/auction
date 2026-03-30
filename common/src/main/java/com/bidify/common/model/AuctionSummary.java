package com.bidify.common.model;

public class AuctionSummary {
    private String id;
    private String auctionName;
    private String description;
    private String seller;
    private String endTime;
    private double startingPrice;
    private double currentBid;
    private int bidCount;

    public AuctionSummary() {
    }

    public AuctionSummary(String id, String auctionName, String description, String seller, String endTime,
            double startingPrice, double currentBid, int bidCount) {
        this.id = id;
        this.auctionName = auctionName;
        this.description = description;
        this.seller = seller;
        this.endTime = endTime;
        this.startingPrice = startingPrice;
        this.currentBid = currentBid;
        this.bidCount = bidCount;
    }

    public String getId() {
        return id;
    }

    public String getAuctionName() {
        return auctionName;
    }

    public String getDescription() {
        return description;
    }

    public String getSeller() {
        return seller;
    }

    public String getEndTime() {
        return endTime;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public int getBidCount() {
        return bidCount;
    }
}

package com.bidify.common.model;

public class InventoryItemSummary {
    private String id;
    private String auctionId;
    private String auctionName;
    private String description; // mô tả
    private String seller;
    private String owner;
    private double wonPrice;
    private String wonAt;

    public InventoryItemSummary() {
    }

    public InventoryItemSummary(String id, String auctionId, String auctionName, String description,
            String seller, String owner, double wonPrice, String wonAt) {
        this.id = id;
        this.auctionId = auctionId;
        this.auctionName = auctionName;
        this.description = description;
        this.seller = seller;
        this.owner = owner;
        this.wonPrice = wonPrice;
        this.wonAt = wonAt;
    }

    public String getId() {
        return id;
    }

    public String getAuctionId() {
        return auctionId;
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

    public String getOwner() {
        return owner;
    }

    public double getWonPrice() {
        return wonPrice;
    }

    public String getWonAt() {
        return wonAt;
    }
}

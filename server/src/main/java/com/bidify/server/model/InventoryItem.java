package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.server.utility.IdGenerator;

public class InventoryItem {
    private final String id;
    private final String auctionId;
    private final String ownerUsername;
    private final String auctionName;
    private final String description;
    private final String sellerUsername;
    private final double wonPrice;
    private final String wonAt;

    public InventoryItem(Auction auction, String ownerUsername) {
        this(
            IdGenerator.genItemId(),
            auction.getId(),
            ownerUsername,
            auction.getAuctionName(),
            auction.getDescription(),
            auction.getSeller(),
            auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice(),
            LocalDateTime.now().toString()
        );
    }

    public InventoryItem(String id, String auctionId, String ownerUsername, String auctionName, String description,
            String sellerUsername, double wonPrice, String wonAt) {
        this.id = id;
        this.auctionId = auctionId;
        this.ownerUsername = ownerUsername;
        this.auctionName = auctionName;
        this.description = description;
        this.sellerUsername = sellerUsername;
        this.wonPrice = wonPrice;
        this.wonAt = wonAt;
    }

    public String getId() {
        return id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public String getAuctionName() {
        return auctionName;
    }

    public String getDescription() {
        return description;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public double getWonPrice() {
        return wonPrice;
    }

    public String getWonAt() {
        return wonAt;
    }
}

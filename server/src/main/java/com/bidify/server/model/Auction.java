package com.bidify.server.model;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.utility.IdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private String auctionName, productType, category, description;
    private double startingPrice = 0, currentBid = 0, minIncrement = 0;
    private AuctionStatus status = AuctionStatus.ACTIVE;
    private String sellerUsername, currentBidderUsername;
    private LocalDateTime endTime, startTime;
    private List<Bid> bids = new ArrayList<>();

    public Auction(String id) { super(id); } // dùng khi load từ sql database

    public Auction(String sellerUsername, String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(IdGenerator.genAuctionId());
        this.sellerUsername = sellerUsername;
        this.auctionName = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public synchronized boolean placeBid(Bid bid) {
        if (bid == null || !isActive()) return false;

        double minAllowed = (currentBid > 0 ? currentBid : startingPrice) + minIncrement;
        if (bid.getAmount() < minAllowed) return false;

        this.currentBid = bid.getAmount();
        this.currentBidderUsername = bid.getBidderUsername();
        this.bids.add(bid);

        return true;
    }
    
    public String getAuctionName() { return auctionName; }
    public void setAuctionName(String name) {this.auctionName = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double price) {this.startingPrice = price; }

    public double getCurrentBid() { return currentBid; }
    public void setCurrentBid(double bid) { this.currentBid = bid; }

    public String getCurrentBidderUsername() { return currentBidderUsername; }
    public void setCurrentBidderUsername(String username) { this.currentBidderUsername = username; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime start) { this.startTime = start; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime time) { this.endTime = time; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public boolean isActive(){ return this.status == AuctionStatus.ACTIVE; }
    public boolean isEnded() { return this.status == AuctionStatus.ENDED; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String username) { this.sellerUsername = username; }

    public int getBidCount(){ return bids.size(); }
    
    public String getProductType(){ return productType; }
    public void setProductType(String type){ this.productType = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double num) { this.minIncrement = num; }

    public List<Bid> getBids() { return bids; }
}

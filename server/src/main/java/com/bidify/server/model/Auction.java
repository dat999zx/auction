package com.bidify.server.model;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.utility.IdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private final String id; 
    private String auctionName, productType, category, description;
    private double startingPrice, currentBid, minIncrement = 0, maxIncrement = 0;
    private AuctionStatus status = AuctionStatus.ACTIVE;
    private String seller;
    User currentBidder;
    private LocalDateTime endTime, startTime;
    private int bidCount;
    private List<Bid> bids = new ArrayList<>();

    public Auction(){ this.id = IdGenerator.genAuctionId();}

    public Auction(String seller, String name, String description, double startingPrice, LocalDateTime startTime, LocalDateTime endTime){
        this.seller = seller;
        this.auctionName = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.id = IdGenerator.genAuctionId();

    }
    
    public synchronized void placeBid(Bid bid){
        this.currentBid = bid.getAmount();
        this.currentBidder = bid.getBidder();
        this.bids.add(bid);
    }

    public String getId(){ return id; }
    
    public String getAuctionName() { return auctionName; }
    public void setAuctionName(String name) {this.auctionName = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double price) {this.startingPrice = price; }

    public double getCurrentBid() { return currentBid; }
    public void setCurrentBid(double bid) { this.currentBid = bid; }

    public User getCurrentBidder() { return currentBidder; }
    public void setCurrentBidder(User person) { this.currentBidder = person; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime start) { this.startTime = start; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime time) { this.endTime = time; }

    public AuctionStatus getStatus() { return status; }
    public void setAuctionStatus(AuctionStatus status) { this.status = status; }
    public boolean isActive(){ return this.status == AuctionStatus.ACTIVE; }
    public boolean isEnded() { return this.status == AuctionStatus.ENDED; }

    public String getSeller() { return seller; }
    public void setSeller(String person) { this.seller = person; }

    public int getBidCount(){ return bidCount; }
    public void setBidCouint(int count) {this.bidCount = count; }
    public void incrementBidCount() { this.bidCount++; }
    
    public String getProductType(){ return productType; }
    public void setProductType(String type){ this.productType = type; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double num) { this.minIncrement = num; }

    public double getMaxIncrement() { return maxIncrement; }
    public void setMaxIncrement(double num) { this.maxIncrement = num; }

    public List<Bid> getBids() { return bids; }
}

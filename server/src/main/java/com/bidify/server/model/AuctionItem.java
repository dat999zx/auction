package com.bidify.server.model;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.utility.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

public class AuctionItem {
    private final String id = IdGenerator.genAuctionId();
    private String AuctionName, productType, category, description;
    private double startingPrice, currentBid, minIncrement = 0, maxIncrement = 0;
    private AuctionStatus status = AuctionStatus.ACTIVE;
    private User seller, currentBidder;
    private LocalDateTime endTime;
    private int bidCount;
    private List<Bid> bids = new ArrayList<>();

    public AuctionItem(){}

    public AuctionItem(String name, String description, double startingPrice, LocalDateTime endTime){
        this.AuctionName = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
    }
    
    public synchronized void placeBid(Bid bid){
        this.currentBid = bid.getAmount();
        this.currentBidder = bid.getBidder();
        this.bids.add(bid);
    }

    public String getId(){ return id; }
    
    public String getAuctionName() { return AuctionName; }
    public void setAuctionName(String name) {this.AuctionName = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double price) {this.startingPrice = price; }

    public double getCurrentBid() { return currentBid; }
    public void setCurrentBid(double bid) { this.currentBid = bid; }

    public User getCurrentBidder() { return currentBidder; }
    public void setCurrentBidder(User person) { this.currentBidder = person; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime time) { this.endTime = time; }

    public AuctionStatus getStatus() { return status; }
    public void setAuctionStatus(AuctionStatus status) { this.status = status; }
    public boolean isActive(){ return this.status == AuctionStatus.ACTIVE; }
    public boolean isEnded() { return this.status == AuctionStatus.ENDED; }

    public User getSeller() { return seller; }
    public void setSeller(User person) { this.seller = person; }

    public int getBidCount(){ return bidCount; }
    public void setBidCouint(int count) {this.bidCount = count; }
    public void incrementBidCount() { this.bidCount++; }
    
    public String getProductType(){ return productType; }
    public void setProductType(String type){ this.productType = type; }

    public String setCategory() { return category; }
    public void getCategory(String category) { this.category = category; }

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double num) { this.minIncrement = num; }

    public double getMaxIncrement() { return maxIncrement; }
    public void setMaxIncrement(double num) { this.maxIncrement = num; }

    public List<Bid> getBids() { return bids; }
}

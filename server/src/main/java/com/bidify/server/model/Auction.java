package com.bidify.server.model;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.BidException;
import com.bidify.common.utility.IdGenerator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private String auctionName, description, sellerUsername, currentBidderUsername, category, productType;
    private double startingPrice = 0, currentBid = 0, minIncrement = 0;
    private AuctionStatus status = AuctionStatus.ACTIVE;
    private LocalDateTime endTime, startTime;
    private List<Bid> bids = new ArrayList<>();

    public Auction(String auctionName, String description, String sellerUsername, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(IdGenerator.genAuctionId(), LocalDateTime.now());
        this.auctionName = auctionName;
        this.description = description;
        this.sellerUsername = sellerUsername;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        refreshStatus();
    }
    
    public Auction(String id, LocalDateTime createdAt, String auctionName, String description, String sellerUsername, String currentBidderUsername, String category, String productType, double startingPrice, double minIncrement, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
        super(id, createdAt);
        this.auctionName = auctionName;
        this.description = description;
        this.sellerUsername = sellerUsername;
        this.currentBidderUsername = currentBidderUsername;
        this.category = category;
        this.productType = productType;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status == null ? AuctionStatus.UPCOMING : status;
        refreshStatus();
    }
    
    public synchronized void placeBid(Bid bid) {
        if (bid == null)
            throw new BidException("Invalid bid");
        if (!isActive())
            throw new AuctionException("Inactive Auction");

        double minAllowed = (currentBid > 0 ? currentBid : startingPrice) + minIncrement;
        if (bid.getAmount() < minAllowed)
            throw new BidException("Bid must be at least " + minAllowed);

        Duration remaining = Duration.between(LocalDateTime.now(), endTime);
        if (remaining.toSeconds() < 30)
            this.endTime = this.endTime.plusSeconds(60);

        this.currentBid = bid.getAmount();
        this.currentBidderUsername = bid.getBidderUsername();
        this.bids.add(bid);
    }

    public boolean refreshStatus() {
        if (status == AuctionStatus.PAID || status == AuctionStatus.BANNED) return false;

        LocalDateTime now = LocalDateTime.now();
        AuctionStatus next;

        if (!now.isBefore(endTime)) next = AuctionStatus.ENDED;
        else if (now.isBefore(startTime)) next = AuctionStatus.UPCOMING;
        else next = AuctionStatus.ACTIVE;

        if (next == status) return false;
        status = next;
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

    public AuctionStatus getStatus() {
        refreshStatus();
        return status;
    }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public boolean isActive(){ return getStatus() == AuctionStatus.ACTIVE; }
    public boolean isEnded() { return getStatus() == AuctionStatus.ENDED; }
    public boolean isUpcoming() { return getStatus() == AuctionStatus.UPCOMING; }

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

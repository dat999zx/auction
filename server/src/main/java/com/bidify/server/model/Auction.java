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
    private String auctionName;
    private String description;
    private String sellerUsername;
    private String itemId;
    private String currentBidderUsername;
    private double startingPrice = 0, currentBid = 0, minIncrement = 0;
    private AuctionStatus status = AuctionStatus.ACTIVE;
    private LocalDateTime endTime, startTime;
    private List<Bid> bids = new ArrayList<>();

    public Auction(String sellerUsername, String itemId, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(IdGenerator.genAuctionId(), LocalDateTime.now());
        this.sellerUsername = sellerUsername;
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;

        if (LocalDateTime.now().isBefore(startTime))
            this.status = AuctionStatus.UPCOMING;
        else
            this.status = AuctionStatus.ACTIVE;
    }

    public Auction(String auctionName, String description, String sellerUsername, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this(sellerUsername, null, startingPrice, startTime, endTime);
        this.auctionName = auctionName;
        this.description = description;
    }
    
    public Auction(String id, LocalDateTime createdAt, String auctionName, String description, String sellerUsername, String itemId, String currentBidderUsername, double startingPrice, double minIncrement, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
        super(id, createdAt);
        this.auctionName = auctionName;
        this.description = description;
        this.sellerUsername = sellerUsername;
        this.itemId = itemId;
        this.currentBidderUsername = currentBidderUsername;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status == null ? AuctionStatus.UPCOMING : status;
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

    public boolean isActive(){ 
        return status == AuctionStatus.ACTIVE && !LocalDateTime.now().isAfter(endTime); 
    }
    public boolean isEnded() { 
        return status == AuctionStatus.ENDED || status == AuctionStatus.CANCELED; 
    }
    public boolean isUpcoming() { return status == AuctionStatus.UPCOMING; }

    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String username) { this.sellerUsername = username; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public int getBidCount(){ return bids.size(); }
    
    public String getProductType(){ return null; }
    public void setProductType(String type){}

    public String getCategory() { return null; }
    public void setCategory(String category) {}

    public double getMinIncrement() { return minIncrement; }
    public void setMinIncrement(double num) { this.minIncrement = num; }

    public List<Bid> getBids() { return bids; }
}

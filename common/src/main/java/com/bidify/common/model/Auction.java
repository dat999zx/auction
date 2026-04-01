package com.bidify.common.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.enums.AuctionStatus;

/**
 * Auction - Represents an auction session for an item
 */
public class Auction extends Entity {
    private static final long serialVersionUID = 1L;
    
    private String itemId;
    private String sellerId;
    private double currentBidPrice;
    private String highestBidderId;
    private int totalBids;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<String> bidderIds;
    private double reservePrice; // Giá tối thiểu mà seller chấp nhận
    private boolean autoExtend; // Liệu đấu giá có tự động kéo dài endTime khi có bid mới gần cuối không
    
    public Auction() {
        super();
        this.currentBidPrice = 0.0;
        this.totalBids = 0;
        this.status = AuctionStatus.PENDING; // Đang chờ
        this.bidderIds = new ArrayList<>();
        this.autoExtend = false;
    }
    
    public Auction(String itemId, String sellerId, double startingPrice) {
        super();
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.currentBidPrice = startingPrice;
        this.totalBids = 0;
        this.status = AuctionStatus.PENDING;
        this.bidderIds = new ArrayList<>();
        this.autoExtend = false;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public double getCurrentBidPrice() {
        return currentBidPrice;
    }
    
    public void setCurrentBidPrice(double currentBidPrice) {
        this.currentBidPrice = currentBidPrice;
    }
    
    public String getHighestBidderId() {
        return highestBidderId;
    }
    
    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }
    
    public int getTotalBids() {
        return totalBids;
    }
    
    public void setTotalBids(int totalBids) {
        this.totalBids = totalBids;
    }
    
    public void incrementBidCount() {
        this.totalBids++;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public AuctionStatus getStatus() {
        return status;
    }
    
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
    
    public List<String> getBidderIds() {
        return bidderIds;
    }
    
    public void addBidder(String bidderId) {
        if (!bidderIds.contains(bidderId)) {
            bidderIds.add(bidderId);
        }
    }
    
    public void removeBidder(String bidderId) {
        bidderIds.remove(bidderId);
    }
    
    public double getReservePrice() {
        return reservePrice;
    }
    
    public void setReservePrice(double reservePrice) {
        this.reservePrice = reservePrice;
    }
    
    public boolean isAutoExtend() {
        return autoExtend;
    }
    
    public void setAutoExtend(boolean autoExtend) {
        this.autoExtend = autoExtend;
    }
    
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == AuctionStatus.ACTIVE && 
               now.isAfter(startTime) && 
               now.isBefore(endTime);
    }
}

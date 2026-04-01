package com.bidify.common.model;

import java.time.LocalDateTime;

/**
 * BidTransaction - Represents a single bid placed in an auction
 */
public class BidTransaction extends Entity {
    private static final long serialVersionUID = 1L;
    
    private String auctionId;
    private String bidderId;
    private String itemId;
    private double bidAmount; // Số tiền mà bidder đặt cho lần bid này
    private LocalDateTime bidTime; // Thời gian chính xác khi bidder đặt giá
    private String bidStatus; // Trạng thái của bid này (còn hiệu lực hay không)
    
    public BidTransaction() {
        super();
        this.bidTime = LocalDateTime.now();
        this.bidStatus = "ACTIVE";
    }
    
    public BidTransaction(String auctionId, String bidderId, String itemId, double bidAmount) {
        super();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.itemId = itemId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
        this.bidStatus = "ACTIVE";
    }
    
    public String getAuctionId() {
        return auctionId;
    }
    
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }
    
    public String getBidderId() {
        return bidderId;
    }
    
    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public double getBidAmount() {
        return bidAmount;
    }
    
    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
    
    public LocalDateTime getBidTime() {
        return bidTime;
    }
    
    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
    
    public String getBidStatus() {
        return bidStatus;
    }
    
    public void setBidStatus(String bidStatus) {
        this.bidStatus = bidStatus;
    }
    
    @Override
    public String toString() {
        return "BidTransaction{" +
                "id='" + id + '\'' +
                ", auctionId='" + auctionId + '\'' +
                ", bidderId='" + bidderId + '\'' +
                ", bidAmount=" + bidAmount +
                ", bidTime=" + bidTime +
                ", bidStatus='" + bidStatus + '\'' +
                '}';
    }
}

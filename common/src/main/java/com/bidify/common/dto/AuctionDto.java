package com.bidify.common.dto;

import java.util.List;

public class AuctionDto {
    private String id;
    private String createdAt;
    private String auctionName;
    private String description;
    private String sellerUsername;
    private String currentBidderUsername;
    private String category;
    private String productType;
    private double startingPrice;
    private double currentBid;
    private double minIncrement;
    private String startTime;
    private String endTime;
    private String status;
    private String thumbnailBase64;
    private List<String> galleryBase64;
    private List<BidDto> bidHistory;

    public AuctionDto(String id, String createdAt, String auctionName, String description, String sellerUsername, 
                      String currentBidderUsername, String category, String productType, double startingPrice, 
                      double currentBid, double minIncrement, String startTime, String endTime, String status) {
        this.id = id;
        this.createdAt = createdAt;
        this.auctionName = auctionName;
        this.description = description;
        this.sellerUsername = sellerUsername;
        this.currentBidderUsername = currentBidderUsername;
        this.category = category;
        this.productType = productType;
        this.startingPrice = startingPrice;
        this.currentBid = currentBid;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public String getId() { return id; }
    public String getCreatedAt() { return createdAt; }
    public String getAuctionName() { return auctionName; }
    public String getDescription() { return description; }
    public String getSellerUsername() { return sellerUsername; }
    public String getCurrentBidderUsername() { return currentBidderUsername; }
    public String getCategory() { return category; }
    public String getProductType() { return productType; }
    public double getStartingPrice() { return startingPrice; }
    public double getCurrentBid() { return currentBid; }
    public double getMinIncrement() { return minIncrement; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getStatus() { return status; }

    public String getThumbnailBase64() { return thumbnailBase64; }
    public void setThumbnailBase64(String thumbnailBase64) { this.thumbnailBase64 = thumbnailBase64; }

    public List<String> getGalleryBase64() { return galleryBase64; }
    public void setGalleryBase64(List<String> galleryBase64) { this.galleryBase64 = galleryBase64; }

    public List<BidDto> getBidHistory() { return bidHistory; }
    public void setBidHistory(List<BidDto> bidHistory) { this.bidHistory = bidHistory; }
}

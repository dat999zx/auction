package com.bidify.common.dto;

import java.util.List;

public class AuctionDto {
    private String id;
    private String itemId;
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
    private boolean currentUserAutoBidActive;
    private Double currentUserAutoBidMax;
    private int watcherCount;
    private int activeBidderCount;

    public AuctionDto(String id, String itemId, String createdAt, String auctionName, String description, String sellerUsername, 
                      String currentBidderUsername, String category, String productType, double startingPrice, 
                      double currentBid, double minIncrement, String startTime, String endTime, String status) {
        this.id = id;
        this.itemId = itemId;
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

    // dùng để lấy ID
    public String getId() { return id; }
    // dùng để lấy sản phẩm ID
    public String getItemId() { return itemId; }
    // dùng để lấy created tại
    public String getCreatedAt() { return createdAt; }
    // dùng để lấy đấu giá tên
    public String getAuctionName() { return auctionName; }
    // dùng để lấy description
    public String getDescription() { return description; }
    // dùng để lấy seller username
    public String getSellerUsername() { return sellerUsername; }
    // dùng để lấy current bidder username
    public String getCurrentBidderUsername() { return currentBidderUsername; }
    // dùng để lấy category
    public String getCategory() { return category; }
    // dùng để lấy product type
    public String getProductType() { return productType; }
    // dùng để lấy starting giá sản phẩm
    public double getStartingPrice() { return startingPrice; }
    // dùng để lấy current lượt đặt giá
    public double getCurrentBid() { return currentBid; }
    // dùng để lấy min increment
    public double getMinIncrement() { return minIncrement; }
    // dùng để lấy bắt đầu thời gian
    public String getStartTime() { return startTime; }
    // dùng để lấy end thời gian
    public String getEndTime() { return endTime; }
    // dùng để lấy trạng thái
    public String getStatus() { return status; }

    // dùng để lấy thumbnail base64
    public String getThumbnailBase64() { return thumbnailBase64; }
    // dùng để thiết lập thumbnail base64
    public void setThumbnailBase64(String thumbnailBase64) { this.thumbnailBase64 = thumbnailBase64; }

    // dùng để lấy gallery base64
    public List<String> getGalleryBase64() { return galleryBase64; }
    // dùng để thiết lập gallery base64
    public void setGalleryBase64(List<String> galleryBase64) { this.galleryBase64 = galleryBase64; }

    // dùng để lấy lượt đặt giá lịch sử
    public List<BidDto> getBidHistory() { return bidHistory; }
    // dùng để thiết lập lượt đặt giá lịch sử
    public void setBidHistory(List<BidDto> bidHistory) { this.bidHistory = bidHistory; }

    // dùng để kiểm tra xem current người dùng auto lượt đặt giá active
    public boolean isCurrentUserAutoBidActive() { return currentUserAutoBidActive; }
    // dùng để thiết lập current người dùng auto lượt đặt giá active
    public void setCurrentUserAutoBidActive(boolean currentUserAutoBidActive) { this.currentUserAutoBidActive = currentUserAutoBidActive; }
    // dùng để lấy current người dùng auto lượt đặt giá max
    public Double getCurrentUserAutoBidMax() { return currentUserAutoBidMax; }
    // dùng để thiết lập current người dùng auto lượt đặt giá max
    public void setCurrentUserAutoBidMax(Double currentUserAutoBidMax) { this.currentUserAutoBidMax = currentUserAutoBidMax; }
    // dùng để lấy watcher count
    public int getWatcherCount() { return watcherCount; }
    // dùng để thiết lập watcher count
    public void setWatcherCount(int watcherCount) { this.watcherCount = watcherCount; }
    // dùng để lấy active bidder count
    public int getActiveBidderCount() { return activeBidderCount; }
    // dùng để thiết lập active bidder count
    public void setActiveBidderCount(int activeBidderCount) { this.activeBidderCount = activeBidderCount; }
}

package com.bidify.server.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.BidException;
import com.bidify.common.utility.IdGenerator;
import com.bidify.common.utility.TimeUtil;

public class Auction extends Entity {
    private String auctionName;
    private String description;
    private String sellerUsername;
    private String itemId;
    private String currentBidderUsername;
    private double startingPrice = 0, currentBid = 0, minIncrement = 0;
    private AuctionStatus status = AuctionStatus.ACTIVE;
    private LocalDateTime endTime, startTime, maxEndTime;
    private Duration antiSnipingTriggerTime = Duration.ZERO; // minTime
    private Duration antiSnipingExtensionTime = Duration.ZERO;
    private List<Bid> bids = new ArrayList<>();
    private List<AutoBid> autoBids = new ArrayList<>();

    // dùng để tạo một đối tượng Auction
    public Auction(String sellerUsername, String itemId, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(IdGenerator.genAuctionId(), TimeUtil.nowInVietnam());
        this.sellerUsername = sellerUsername;
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxEndTime = endTime; // default

        if (TimeUtil.nowInVietnam().isBefore(startTime))
            this.status = AuctionStatus.UPCOMING;
        else
            this.status = AuctionStatus.ACTIVE;
    }

    // dùng để tạo một đối tượng Auction
    public Auction(String auctionName, String description, String sellerUsername, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this(sellerUsername, null, startingPrice, startTime, endTime);
        this.auctionName = auctionName;
        this.description = description;
    }
    
    // dùng để tạo một đối tượng Auction
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
        this.maxEndTime = endTime; // default
        this.status = status == null ? AuctionStatus.UPCOMING : status;
    }
    
    // dùng để place lượt đặt giá
    public synchronized void placeBid(Bid bid) {
        if (bid == null)
            throw new BidException("Invalid bid");
        if (!isActive())
            throw new AuctionException("Inactive Auction");

        double minAllowed = (currentBid > 0 ? currentBid : startingPrice) + minIncrement;
        if (bid.getAmount() < minAllowed)
            throw new BidException("Bid must be at least " + minAllowed);

        if (isAntiSnipingConfigured()) {
            // Fix: Use the official bid creation time to calculate true remaining time
            LocalDateTime bidTime = bid.getCreatedAt() != null ? bid.getCreatedAt() : LocalDateTime.now();
            Duration remaining = Duration.between(bidTime, endTime);

            // Fix: Use <= 0 to safely capture when remaining time matches the trigger window exactly down to the nanosecond
            if (remaining.compareTo(antiSnipingTriggerTime) <= 0 && !remaining.isNegative()) {
                
                LocalDateTime newEndTime = endTime.plus(antiSnipingExtensionTime);
                
                if (newEndTime.isAfter(maxEndTime)) {
                    newEndTime = maxEndTime;
                }
                    
                if (newEndTime.isAfter(endTime)) {
                    this.endTime = newEndTime;
                }
            }
        }

        this.currentBid = bid.getAmount();
        this.currentBidderUsername = bid.getBidderUsername();
        this.bids.add(bid);
    }
    // dùng để kiểm tra xem có cấu hình anti-sniping không
    public boolean isAntiSnipingConfigured() {
    return antiSnipingTriggerTime != null && !antiSnipingTriggerTime.isZero() 
        && antiSnipingExtensionTime != null && !antiSnipingExtensionTime.isZero()
        && maxEndTime != null && !maxEndTime.isBefore(endTime); // 2048: Allowed to be equal
    }

    // dùng để lấy đấu giá tên
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

    public LocalDateTime getMaxEndTime() { return maxEndTime; }
    public void setMaxEndTime(LocalDateTime maxEndTime) { this.maxEndTime = maxEndTime; }

    public Duration getAntiSnipingTriggerTime() { return antiSnipingTriggerTime; }
    public void setAntiSnipingTriggerTime(Duration triggerTime) { this.antiSnipingTriggerTime = triggerTime; }

    public Duration getAntiSnipingExtensionTime() { return antiSnipingExtensionTime; }
    public void setAntiSnipingExtensionTime(Duration extensionTime) { this.antiSnipingExtensionTime = extensionTime; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    // dùng để kiểm tra xem active
    public boolean isActive(){ 
        return status == AuctionStatus.ACTIVE && !TimeUtil.nowInVietnam().isAfter(endTime);
    }
    // dùng để kiểm tra xem ended
    public boolean isEnded() { 
        return status == AuctionStatus.ENDED || status == AuctionStatus.CANCELED; 
    }
    // dùng để kiểm tra xem upcoming
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

    // dùng để lấy auto danh sách đặt giá
    public synchronized List<AutoBid> getAutoBids() {
        return new ArrayList<>(autoBids);
    }

    // dùng để lấy auto lượt đặt giá
    public synchronized AutoBid getAutoBid(String username) {
        if (username == null) return null;
        return autoBids.stream()
                .filter(autoBid -> username.equals(autoBid.getUsername()) && autoBid.isEnabled())
                .findFirst()
                .orElse(null);
    }

    // dùng để upsert auto lượt đặt giá
    public synchronized void upsertAutoBid(AutoBid autoBid) {
        autoBids.removeIf(existing -> existing.getUsername().equals(autoBid.getUsername()));
        autoBids.add(autoBid);
    }

    // dùng để disable auto lượt đặt giá
    public synchronized void disableAutoBid(String username) {
        AutoBid autoBid = getAutoBid(username);
        if (autoBid != null) autoBid.disable();
    }
}

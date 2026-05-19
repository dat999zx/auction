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
    private List<AutoBid> autoBids = new ArrayList<>();

    // dùng để tạo một đối tượng Auction
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

    // dùng để tạo một đối tượng Auction
    public Auction(String auctionName, String description, String sellerUsername, double startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        // dùng để this
        this(sellerUsername, null, startingPrice, startTime, endTime);
        this.auctionName = auctionName;
        this.description = description;
    }
    
    // dùng để tạo một đối tượng Auction
    public Auction(String id, LocalDateTime createdAt, String auctionName, String description, String sellerUsername, String itemId, String currentBidderUsername, double startingPrice, double minIncrement, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status) {
        // dùng để super
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
    
    // dùng để place lượt đặt giá
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
    
    // dùng để lấy đấu giá tên
    public String getAuctionName() { return auctionName; }
    // dùng để thiết lập đấu giá tên
    public void setAuctionName(String name) {this.auctionName = name; }

    // dùng để lấy description
    public String getDescription() { return description; }
    // dùng để thiết lập description
    public void setDescription(String description) { this.description = description; }
    
    // dùng để lấy starting giá sản phẩm
    public double getStartingPrice() { return startingPrice; }
    // dùng để thiết lập starting giá sản phẩm
    public void setStartingPrice(double price) {this.startingPrice = price; }

    // dùng để lấy current lượt đặt giá
    public double getCurrentBid() { return currentBid; }
    // dùng để thiết lập current lượt đặt giá
    public void setCurrentBid(double bid) { this.currentBid = bid; }

    // dùng để lấy current bidder username
    public String getCurrentBidderUsername() { return currentBidderUsername; }
    // dùng để thiết lập current bidder username
    public void setCurrentBidderUsername(String username) { this.currentBidderUsername = username; }

    // dùng để lấy bắt đầu thời gian
    public LocalDateTime getStartTime() { return startTime; }
    // dùng để thiết lập bắt đầu thời gian
    public void setStartTime(LocalDateTime start) { this.startTime = start; }

    // dùng để lấy end thời gian
    public LocalDateTime getEndTime() { return endTime; }
    // dùng để thiết lập end thời gian
    public void setEndTime(LocalDateTime time) { this.endTime = time; }

    // dùng để lấy trạng thái
    public AuctionStatus getStatus() { return status; }
    // dùng để thiết lập trạng thái
    public void setStatus(AuctionStatus status) { this.status = status; }

    // dùng để kiểm tra xem active
    public boolean isActive(){ 
        return status == AuctionStatus.ACTIVE && !LocalDateTime.now().isAfter(endTime); 
    }
    // dùng để kiểm tra xem ended
    public boolean isEnded() { 
        return status == AuctionStatus.ENDED || status == AuctionStatus.CANCELED; 
    }
    // dùng để kiểm tra xem upcoming
    public boolean isUpcoming() { return status == AuctionStatus.UPCOMING; }

    // dùng để lấy seller username
    public String getSellerUsername() { return sellerUsername; }
    // dùng để thiết lập seller username
    public void setSellerUsername(String username) { this.sellerUsername = username; }

    // dùng để lấy sản phẩm ID
    public String getItemId() { return itemId; }
    // dùng để thiết lập sản phẩm ID
    public void setItemId(String itemId) { this.itemId = itemId; }

    // dùng để lấy lượt đặt giá count
    public int getBidCount(){ return bids.size(); }
    
    // dùng để lấy product type
    public String getProductType(){ return null; }
    // dùng để thiết lập product type
    public void setProductType(String type){}

    // dùng để lấy category
    public String getCategory() { return null; }
    // dùng để thiết lập category
    public void setCategory(String category) {}

    // dùng để lấy min increment
    public double getMinIncrement() { return minIncrement; }
    // dùng để thiết lập min increment
    public void setMinIncrement(double num) { this.minIncrement = num; }

    // dùng để lấy danh sách đặt giá
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

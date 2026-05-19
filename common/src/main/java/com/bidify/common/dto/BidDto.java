package com.bidify.common.dto;

public class BidDto {
    private String id;
    private String createdAt;
    private double amount;
    private String bidderUsername;
    private String auctionId;
    private boolean autoBidGenerated;

    // dùng để tạo một đối tượng BidDto
    public BidDto() {}

    // dùng để tạo một đối tượng BidDto
    public BidDto(String id, String createdAt, String auctionId, String bidderUsername, double amount) {
        // dùng để this
        this(id, createdAt, auctionId, bidderUsername, amount, false);
    }

    // dùng để tạo một đối tượng BidDto
    public BidDto(String id, String createdAt, String auctionId, String bidderUsername, double amount, boolean autoBidGenerated) {
        this.id = id;
        this.createdAt = createdAt;
        this.auctionId = auctionId;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.autoBidGenerated = autoBidGenerated;
    }

    // dùng để lấy ID
    public String getId() {
        return id;
    }

    // dùng để lấy created tại
    public String getCreatedAt() {
        return createdAt;
    }

    // dùng để lấy số tiền
    public double getAmount() {
        return amount;
    }

    // dùng để lấy bidder username
    public String getBidderUsername() {
        return bidderUsername;
    }

    // dùng để lấy đấu giá ID
    public String getAuctionId() {
        return auctionId;
    }

    // dùng để kiểm tra xem auto lượt đặt giá generated
    public boolean isAutoBidGenerated() {
        return autoBidGenerated;
    }
}

package com.bidify.common.model;

// Yêu cầu lấy thông tin chi tiết 1 phiên đấu giá
public class GetAuctionDetailRequest {
    // ID phiên đấu giá muốn xem chi tiết
    private String auctionId;

    public GetAuctionDetailRequest(String auctionId) { this.auctionId = auctionId; }

    public String getAuctionId() { return auctionId; }
}

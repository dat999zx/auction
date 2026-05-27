package com.bidify.common.model;

// Yêu cầu xóa 1 phiên đấu giá
public class DeleteAuctionRequest {
    // ID phiên đấu giá cần xóa
    private String id;
    
    public DeleteAuctionRequest(String id){
        this.id = id;
    }

    public String getId() { return id; }
}

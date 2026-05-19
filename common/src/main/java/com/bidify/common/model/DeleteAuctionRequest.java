package com.bidify.common.model;

public class DeleteAuctionRequest {
    private String id;
    // dùng để tạo một đối tượng DeleteAuctionRequest
    public DeleteAuctionRequest(String id){
        this.id = id;
    }

    // dùng để lấy ID
    public String getId() { return id; }
}

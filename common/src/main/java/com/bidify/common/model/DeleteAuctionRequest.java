package com.bidify.common.model;

public class DeleteAuctionRequest {
    private String id;
    
    public DeleteAuctionRequest(String id){
        this.id = id;
    }

    public String getId() { return id; }
}

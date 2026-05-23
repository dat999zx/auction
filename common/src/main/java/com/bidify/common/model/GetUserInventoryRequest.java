package com.bidify.common.model;

public class GetUserInventoryRequest {
    private String ownerUsername;

    public GetUserInventoryRequest(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerUsername() { return ownerUsername; }
}

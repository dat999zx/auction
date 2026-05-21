package com.bidify.common.model;

public class GetUserInventoryRequest {
    private String ownerUsername;

    // dùng để tạo một đối tượng GetUserInventoryRequest
    public GetUserInventoryRequest(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerUsername() { return ownerUsername; }
}

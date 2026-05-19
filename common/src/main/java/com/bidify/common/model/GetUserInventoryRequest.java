package com.bidify.common.model;

public class GetUserInventoryRequest {
    private String ownerUsername;

    // dùng để tạo một đối tượng GetUserInventoryRequest
    public GetUserInventoryRequest(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    // dùng để lấy chủ sở hữu username
    public String getOwnerUsername() { return ownerUsername; }
}

package com.bidify.common.model;

// Yêu cầu lấy danh sách vật phẩm trong kho của 1 user cụ thể (xem kho người khác)
public class GetUserInventoryRequest {
    // Username chủ kho muốn xem
    private String ownerUsername;

    public GetUserInventoryRequest(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerUsername() { return ownerUsername; }
}

package com.bidify.common.model;

public class GetItemDetailRequest {
    private String itemId;

    // dùng để tạo một đối tượng GetItemDetailRequest
    public GetItemDetailRequest(String itemId) {
        this.itemId = itemId;
    }

    public String getItemId() { return itemId; }
}

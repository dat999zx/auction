package com.bidify.common.model;

public class GetItemDetailRequest {
    private String itemId;

    // dùng để tạo một đối tượng GetItemDetailRequest
    public GetItemDetailRequest(String itemId) {
        this.itemId = itemId;
    }

    // dùng để lấy sản phẩm ID
    public String getItemId() { return itemId; }
}

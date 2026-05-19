package com.bidify.common.model;

public class DeleteItemRequest {
    private String itemId;

    // dùng để tạo một đối tượng DeleteItemRequest
    public DeleteItemRequest(String itemId) {
        this.itemId = itemId;
    }

    // dùng để lấy sản phẩm ID
    public String getItemId() { return itemId; }
}

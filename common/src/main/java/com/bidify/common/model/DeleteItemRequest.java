package com.bidify.common.model;

// Yêu cầu xóa vật phẩm khỏi kho
public class DeleteItemRequest {
    // ID vật phẩm cần xóa
    private String itemId;

    public DeleteItemRequest(String itemId) {
        this.itemId = itemId;
    }

    public String getItemId() { return itemId; }
}

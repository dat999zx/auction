package com.bidify.common.model;

// Yêu cầu lấy thông tin chi tiết 1 vật phẩm
public class GetItemDetailRequest {
    // ID vật phẩm muốn xem chi tiết
    private String itemId;

    public GetItemDetailRequest(String itemId) {
        this.itemId = itemId;
    }

    public String getItemId() { return itemId; }
}

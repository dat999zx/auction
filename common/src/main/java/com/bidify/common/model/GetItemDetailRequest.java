package com.bidify.common.model;

public class GetItemDetailRequest {
    private String itemId;

    public GetItemDetailRequest(String itemId) {
        this.itemId = itemId;
    }

    public String getItemId() { return itemId; }
}

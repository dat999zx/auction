package com.bidify.common.model;

public class DeleteItemRequest {
    private String itemId;

    public DeleteItemRequest(String itemId) {
        this.itemId = itemId;
    }

    public String getItemId() { return itemId; }
}

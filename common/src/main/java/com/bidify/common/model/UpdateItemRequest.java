package com.bidify.common.model;

import java.util.List;

public class UpdateItemRequest {
    private String itemId;
    private String ownerUsername;
    private String name;
    private String description;
    private String category;
    private String productType;
    private List<String> imagesBase64;

    public UpdateItemRequest(String itemId, String ownerUsername, String name, String description,
            String category, String productType, List<String> imagesBase64) {
        this.itemId = itemId;
        this.ownerUsername = ownerUsername;
        this.name = name;
        this.description = description;
        this.category = category;
        this.productType = productType;
        this.imagesBase64 = imagesBase64;
    }

    public String getItemId() { return itemId; }

    public String getOwnerUsername() { return ownerUsername; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getCategory() { return category; }

    public String getProductType() { return productType; }

    public List<String> getImagesBase64() { return imagesBase64; }
}

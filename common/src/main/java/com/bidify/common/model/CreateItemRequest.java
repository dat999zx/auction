package com.bidify.common.model;

import java.util.List;

public class CreateItemRequest {
    private String ownerUsername;
    private String name;
    private String description;
    private String category;
    private String productType;
    private List<String> imagesBase64;

    public CreateItemRequest(String ownerUsername, String name, String description, String category, String productType,
            List<String> imagesBase64) {
        this.ownerUsername = ownerUsername;
        this.name = name;
        this.description = description;
        this.category = category;
        this.productType = productType;
        this.imagesBase64 = imagesBase64;
    }

    // dùng để lấy chủ sở hữu username
    public String getOwnerUsername() { return ownerUsername; }

    // dùng để lấy tên
    public String getName() { return name; }

    // dùng để lấy description
    public String getDescription() { return description; }

    // dùng để lấy category
    public String getCategory() { return category; }

    // dùng để lấy product type
    public String getProductType() { return productType; }

    // dùng để lấy images base64
    public List<String> getImagesBase64() { return imagesBase64; }
}

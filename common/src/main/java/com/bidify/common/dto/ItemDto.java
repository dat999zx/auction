package com.bidify.common.dto;

import java.util.List;

public class ItemDto {
    private String id;
    private String createdAt;
    private String ownerUsername;
    private String name;
    private String description;
    private String category;
    private String productType;
    private String availabilityStatus;
    private String thumbnailBase64;
    private List<String> galleryBase64;

    public ItemDto(
        String id,
        String createdAt,
        String ownerUsername,
        String name,
        String description,
        String category,
        String productType,
        String availabilityStatus
    ) {
        this.id = id;
        this.createdAt = createdAt;
        this.ownerUsername = ownerUsername;
        this.name = name;
        this.description = description;
        this.category = category;
        this.productType = productType;
        this.availabilityStatus = availabilityStatus;
    }

    public String getId() { return id; }

    public String getCreatedAt() { return createdAt; }

    public String getOwnerUsername() { return ownerUsername; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getCategory() { return category; }

    public String getProductType() { return productType; }

    public String getAvailabilityStatus() { return availabilityStatus; }

    public String getThumbnailBase64() { return thumbnailBase64; }

    public void setThumbnailBase64(String thumbnailBase64) { this.thumbnailBase64 = thumbnailBase64; }

    public List<String> getGalleryBase64() { return galleryBase64; }

    public void setGalleryBase64(List<String> galleryBase64) { this.galleryBase64 = galleryBase64; }
}

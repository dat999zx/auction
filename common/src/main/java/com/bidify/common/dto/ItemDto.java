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

    // dùng để lấy ID
    public String getId() { return id; }

    // dùng để lấy created tại
    public String getCreatedAt() { return createdAt; }

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

    // dùng để lấy availability trạng thái
    public String getAvailabilityStatus() { return availabilityStatus; }

    // dùng để lấy thumbnail base64
    public String getThumbnailBase64() { return thumbnailBase64; }

    // dùng để thiết lập thumbnail base64
    public void setThumbnailBase64(String thumbnailBase64) { this.thumbnailBase64 = thumbnailBase64; }

    // dùng để lấy gallery base64
    public List<String> getGalleryBase64() { return galleryBase64; }

    // dùng để thiết lập gallery base64
    public void setGalleryBase64(List<String> galleryBase64) { this.galleryBase64 = galleryBase64; }
}

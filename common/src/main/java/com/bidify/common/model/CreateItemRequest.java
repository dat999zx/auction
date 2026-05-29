package com.bidify.common.model;

import java.util.List;

// Dữ liệu gửi lên khi tạo vật phẩm mới vào kho
public class CreateItemRequest {
    // Username chủ sở hữu vật phẩm
    private String ownerUsername;
    // Tên vật phẩm
    private String name;
    // Mô tả chi tiết
    private String description;
    // Danh mục (Electronics, Clothing...)
    private String category;
    // Loại sản phẩm cụ thể hơn
    private String productType;
    // Danh sách ảnh vật phẩm (encode base64)
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

    public String getOwnerUsername() { return ownerUsername; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getCategory() { return category; }

    public String getProductType() { return productType; }

    public List<String> getImagesBase64() { return imagesBase64; }
}

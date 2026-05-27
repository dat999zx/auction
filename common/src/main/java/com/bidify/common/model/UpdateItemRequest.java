package com.bidify.common.model;

import java.util.List;

// Dữ liệu gửi lên khi chỉnh sửa thông tin vật phẩm
public class UpdateItemRequest {
    // ID vật phẩm cần cập nhật
    private String itemId;
    // Username chủ sở hữu (để xác minh quyền)
    private String ownerUsername;
    // Tên mới
    private String name;
    // Mô tả mới
    private String description;
    // Danh mục mới
    private String category;
    // Loại sản phẩm mới
    private String productType;
    // Danh sách ảnh mới (thay thế toàn bộ ảnh cũ)
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

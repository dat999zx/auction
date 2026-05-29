package com.bidify.server.model;

import java.time.LocalDateTime;

// Bảng liên kết giữa vật phẩm và ảnh — 1 vật phẩm có thể có nhiều ảnh
public class ItemImageLink extends Entity {
    // ID vật phẩm
    private String itemId;
    // ID ảnh tương ứng
    private String imageId;
    // Thứ tự hiển thị (số nhỏ hiển trước)
    private int displayOrder;
    // true nếu đây là ảnh thumbnail đại diện của vật phẩm
    private boolean isPrimary;

    public ItemImageLink(String id, LocalDateTime createdAt, String itemId, String imageId, int displayOrder, boolean isPrimary) {
        super(id, createdAt);
        this.itemId = itemId;
        this.imageId = imageId;
        this.displayOrder = displayOrder;
        this.isPrimary = isPrimary;
    }

    public String getItemId() { return itemId; }
    public String getImageId() { return imageId; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isPrimary() { return isPrimary; }
}

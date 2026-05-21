package com.bidify.server.model;

import java.time.LocalDateTime;

public class ItemImageLink extends Entity {
    private String itemId;
    private String imageId;
    private int displayOrder;
    private boolean isPrimary;

    // dùng để tạo một đối tượng ItemImageLink
    public ItemImageLink(String id, LocalDateTime createdAt, String itemId, String imageId, int displayOrder, boolean isPrimary) {
        // dùng để super
        super(id, createdAt);
        this.itemId = itemId;
        this.imageId = imageId;
        this.displayOrder = displayOrder;
        this.isPrimary = isPrimary;
    }

    public String getItemId() { return itemId; }
    public String getImageId() { return imageId; }
    public int getDisplayOrder() { return displayOrder; }
    // dùng để kiểm tra xem primary
    public boolean isPrimary() { return isPrimary; }
}

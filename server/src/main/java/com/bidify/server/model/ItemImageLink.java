package com.bidify.server.model;

import java.time.LocalDateTime;

public class ItemImageLink extends Entity {
    private String itemId;
    private String imageId;
    private int displayOrder;
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

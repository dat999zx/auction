package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.enums.ItemStatus;
import com.bidify.common.utility.IdGenerator;
import com.bidify.common.utility.TimeUtil;

// Vật phẩm trong kho của user — có thể đem ra đấu giá
public class Item extends Entity {
    // Username chủ sở hữu vật phẩm
    private String ownerUsername;
    // Tên vật phẩm
    private String name;
    // Mô tả chi tiết
    private String description;
    // Danh mục (Electronics, Clothing...)
    private String category;
    // Loại sản phẩm cụ thể hơn trong danh mục
    private String productType;
    // Trạng thái: AVAILABLE (có thể đấu giá) hoặc IN_AUCTION (đang được đem ra đấu giá)
    private ItemStatus availabilityStatus;

    public Item(String ownerUsername, String name, String description, String category, String productType) {
        super(IdGenerator.genItemId(), TimeUtil.nowInVietnam());
        this.ownerUsername = ownerUsername;
        this.name = name;
        this.description = description;
        this.category = category;
        this.productType = productType;
        this.availabilityStatus = ItemStatus.AVAILABLE;
    }

    public Item(
        String id,
        LocalDateTime createdAt,
        String ownerUsername,
        String name,
        String description,
        String category,
        String productType,
        ItemStatus availabilityStatus
    ) {
        super(id, createdAt);
        this.ownerUsername = ownerUsername;
        this.name = name;
        this.description = description;
        this.category = category;
        this.productType = productType;
        this.availabilityStatus = availabilityStatus == null ? ItemStatus.AVAILABLE : availabilityStatus;
    }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getOwner() { return ownerUsername; }
    public void setOwner(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public ItemStatus getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(ItemStatus availabilityStatus) { this.availabilityStatus = availabilityStatus; }
}

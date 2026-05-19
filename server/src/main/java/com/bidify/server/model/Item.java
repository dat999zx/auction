package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.enums.ItemStatus;
import com.bidify.common.utility.IdGenerator;

public class Item extends Entity {
    private String ownerUsername;
    private String name;
    private String description;
    private String category;
    private String productType;
    private ItemStatus availabilityStatus;

    // dùng để tạo một đối tượng Item
    public Item(String ownerUsername, String name, String description, String category, String productType) {
        super(IdGenerator.genItemId(), LocalDateTime.now());
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
        // dùng để super
        super(id, createdAt);
        this.ownerUsername = ownerUsername;
        this.name = name;
        this.description = description;
        this.category = category;
        this.productType = productType;
        this.availabilityStatus = availabilityStatus == null ? ItemStatus.AVAILABLE : availabilityStatus;
    }

    // dùng để lấy chủ sở hữu username
    public String getOwnerUsername() { return ownerUsername; }
    // dùng để thiết lập chủ sở hữu username
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    // dùng để lấy chủ sở hữu
    public String getOwner() { return ownerUsername; }
    // dùng để thiết lập chủ sở hữu
    public void setOwner(String ownerUsername) { this.ownerUsername = ownerUsername; }

    // dùng để lấy tên
    public String getName() { return name; }
    // dùng để thiết lập tên
    public void setName(String name) { this.name = name; }

    // dùng để lấy description
    public String getDescription() { return description; }
    // dùng để thiết lập description
    public void setDescription(String description) { this.description = description; }

    // dùng để lấy category
    public String getCategory() { return category; }
    // dùng để thiết lập category
    public void setCategory(String category) { this.category = category; }

    // dùng để lấy product type
    public String getProductType() { return productType; }
    // dùng để thiết lập product type
    public void setProductType(String productType) { this.productType = productType; }

    // dùng để lấy availability trạng thái
    public ItemStatus getAvailabilityStatus() { return availabilityStatus; }
    // dùng để thiết lập availability trạng thái
    public void setAvailabilityStatus(ItemStatus availabilityStatus) { this.availabilityStatus = availabilityStatus; }
}

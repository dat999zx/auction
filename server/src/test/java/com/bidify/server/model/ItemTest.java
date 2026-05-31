package com.bidify.server.model;

import com.bidify.common.enums.ItemStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn giản cho Item.
 */
public class ItemTest {

    @Test
    public void testItemInitializationWithDefaultConstructor() {
        String owner = "seller_usr";
        String name = "MacBook Air";
        String desc = "M1 2020 8GB RAM";
        String category = "Electronics";
        String type = "Laptop";

        // Khởi động constructor chính
        Item item = new Item(owner, name, desc, category, type);

        // Xác thực thuộc tính tự động
        assertNotNull(item.getId(), "ID phải được tự động sinh");
        assertNotNull(item.getCreatedAt(), "Thời gian tạo không được null");
        assertEquals(owner, item.getOwnerUsername());
        assertEquals(owner, item.getOwner());
        assertEquals(name, item.getName());
        assertEquals(desc, item.getDescription());
        assertEquals(category, item.getCategory());
        assertEquals(type, item.getProductType());
        assertEquals(ItemStatus.AVAILABLE, item.getAvailabilityStatus(), "Trạng thái mặc định phải là AVAILABLE");
    }

    @Test
    public void testItemInitializationWithDatabaseConstructor() {
        String id = "item-db-123";
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 29, 1, 0, 0);
        String owner = "seller_db";
        String name = "Bàn phím cơ";
        String desc = "Keychron K2";
        String category = "Accessories";
        String type = "Keyboard";
        ItemStatus status = ItemStatus.LOCKED_IN_AUCTION;

        // Constructor load từ DB
        Item item = new Item(id, createdAt, owner, name, desc, category, type, status);

        // Xác thực bảo toàn
        assertEquals(id, item.getId());
        assertEquals(createdAt, item.getCreatedAt());
        assertEquals(owner, item.getOwnerUsername());
        assertEquals(name, item.getName());
        assertEquals(desc, item.getDescription());
        assertEquals(category, item.getCategory());
        assertEquals(type, item.getProductType());
        assertEquals(status, item.getAvailabilityStatus());
    }

    @Test
    public void testItemSettersAndStatusChange() {
        Item item = new Item("owner", "Ghế xoay", "Mô tả", "Furniture", "Chair");

        // Thay đổi thông tin bằng setters
        item.setName("Ghế gaming");
        item.setDescription("Mô tả mới");
        item.setCategory("Gaming");
        item.setProductType("Chair-G");
        item.setOwnerUsername("new_owner");
        item.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);

        // Xác thực thay đổi
        assertEquals("Ghế gaming", item.getName());
        assertEquals("Mô tả mới", item.getDescription());
        assertEquals("Gaming", item.getCategory());
        assertEquals("Chair-G", item.getProductType());
        assertEquals("new_owner", item.getOwnerUsername());
        assertEquals("new_owner", item.getOwner());
        assertEquals(ItemStatus.LOCKED_IN_AUCTION, item.getAvailabilityStatus());
    }
}

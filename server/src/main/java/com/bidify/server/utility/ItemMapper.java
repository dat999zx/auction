package com.bidify.server.utility;

import com.bidify.common.dto.ItemDto;
import com.bidify.server.model.Item;

// Chuyển đổi object Item (server) → ItemDto (gửi cho client)
public class ItemMapper {
    private ItemMapper() {}

    public static ItemDto toDto(Item item) {
        if (item == null) return null;

        return new ItemDto(
                item.getId(),
                item.getCreatedAt().toString(),
                item.getOwnerUsername(),
                item.getName(),
                item.getDescription(),
                item.getCategory(),
                item.getProductType(),
                item.getAvailabilityStatus().name()
        );
    }
}

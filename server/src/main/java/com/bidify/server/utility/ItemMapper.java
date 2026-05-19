package com.bidify.server.utility;

import com.bidify.common.dto.ItemDto;
import com.bidify.server.model.Item;

public class ItemMapper {
    // dùng để tạo một đối tượng ItemMapper
    private ItemMapper() {}

    // dùng để chuyển thành đối tượng truyền tải dữ liệu (DTO)
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

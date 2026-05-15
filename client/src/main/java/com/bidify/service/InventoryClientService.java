package com.bidify.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.ItemDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateItemRequest;
import com.bidify.common.model.GetInventoryRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.network.SocketClient;

public class InventoryClientService {
    private final SocketClient client = SocketClient.getClient();

    public List<ItemDto> getMyInventory() throws IOException {
        Response response = client.send(new Request(RequestType.GET_MY_INVENTORY, new GetInventoryRequest()));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new ValidationException(
                response.getMessage() == null ? "Cannot load inventory." : response.getMessage()
            );
        }

        List<?> rawItems = JsonUtil.fromMap(response.getData(), List.class);
        List<ItemDto> inventory = new ArrayList<>();
        if (rawItems == null)
            return inventory;

        for (Object rawItem : rawItems) {
            ItemDto item = JsonUtil.fromMap(rawItem, ItemDto.class);
            if (item != null)
                inventory.add(item);
        }

        return inventory;
    }

    public ItemDto createItem(String name, String description, String category, String productType, List<String> imagesBase64)
            throws IOException {
        String ownerUsername = client.getCurrentUsername();
        ValidationUtil.validateUsername(ownerUsername);
        ValidationUtil.requiresNonBlank(name, "Item name");
        ValidationUtil.requiresNonBlank(description, "Description");
        ValidationUtil.requiresNonBlank(category, "Category");
        ValidationUtil.requiresNonBlank(productType, "Product type");
        ValidationUtil.validateMaxLength("Description", description, 2000);

        Response response = client.send(
            new Request(
                RequestType.CREATE_ITEM,
                new CreateItemRequest(
                    ownerUsername,
                    name,
                    description,
                    category,
                    productType,
                    imagesBase64
                )
            )
        );

        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new ValidationException(response.getMessage() == null ? "Cannot create item." : response.getMessage());
        }

        ItemDto item = JsonUtil.fromMap(response.getData(), ItemDto.class);
        if (item == null) {
            throw new ValidationException("Create item came back in an unexpected format.");
        }

        return item;
    }
}

package com.bidify.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.ItemDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateItemRequest;
import com.bidify.common.model.DeleteItemRequest;
import com.bidify.common.model.GetItemDetailRequest;
import com.bidify.common.model.GetInventoryRequest;
import com.bidify.common.model.GetUserInventoryRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdateItemRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.model.ClientSession;
import com.bidify.network.SocketClient;

public class InventoryClientService {
    private final SocketClient client = SocketClient.getClient();
    private final ClientSession clientSession = ClientSession.getInstance();

    public List<ItemDto> getMyInventory() throws IOException {
        Response response = client.send(new Request(RequestType.GET_MY_INVENTORY, new GetInventoryRequest()));
        return consumeInventoryResponse(response, "Cannot load inventory.");
    }

    public List<ItemDto> getInventoryForOwner(String ownerUsername) throws IOException {
        ValidationUtil.validateUsername(ownerUsername);
        Response response = client.send(new Request(RequestType.GET_USER_INVENTORY, new GetUserInventoryRequest(ownerUsername)));
        return consumeInventoryResponse(response, "Cannot load user inventory.");
    }

    private List<ItemDto> consumeInventoryResponse(Response response, String fallbackMessage) {
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new ValidationException(
                response.getMessage() == null ? fallbackMessage : response.getMessage()
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
        String ownerUsername = clientSession.getCurrentUsername();
        ValidationUtil.validateUsername(ownerUsername);
        validateItemFields(name, description, category, productType);

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

    public ItemDto getItemDetail(String itemId) throws IOException {
        ValidationUtil.requiresNonBlank(itemId, "Item ID");

        Response response = client.send(
            new Request(RequestType.GET_ITEM_DETAIL, new GetItemDetailRequest(itemId))
        );

        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null)
            throw new ValidationException(response.getMessage() == null ? "Cannot load item." : response.getMessage());

        ItemDto item = JsonUtil.fromMap(response.getData(), ItemDto.class);
        if (item == null)
            throw new ValidationException("Item detail came back in an unexpected format.");
        return item;
    }

    public ItemDto updateItem(String itemId, String name, String description, String category, String productType,
            List<String> imagesBase64) throws IOException {
        String ownerUsername = clientSession.getCurrentUsername();
        ValidationUtil.validateUsername(ownerUsername);
        ValidationUtil.requiresNonBlank(itemId, "Item ID");
        validateItemFields(name, description, category, productType);

        Response response = client.send(
            new Request(
                RequestType.UPDATE_ITEM,
                new UpdateItemRequest(itemId, ownerUsername, name, description, category, productType, imagesBase64)
            )
        );

        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null)
            throw new ValidationException(response.getMessage() == null ? "Cannot update item." : response.getMessage());

        ItemDto item = JsonUtil.fromMap(response.getData(), ItemDto.class);
        if (item == null)
            throw new ValidationException("Update item came back in an unexpected format.");
        return item;
    }

    public void deleteItem(String itemId) throws IOException {
        ValidationUtil.requiresNonBlank(itemId, "Item ID");

        Response response = client.send(
            new Request(RequestType.DELETE_ITEM, new DeleteItemRequest(itemId))
        );

        if (response.getStatus() != RequestStatus.SUCCESS)
            throw new ValidationException(response.getMessage() == null ? "Cannot delete item." : response.getMessage());
    }

    private void validateItemFields(String name, String description, String category, String productType) {
        ValidationUtil.requiresNonBlank(name, "Item name");
        ValidationUtil.requiresNonBlank(description, "Description");
        ValidationUtil.requiresNonBlank(category, "Category");
        ValidationUtil.requiresNonBlank(productType, "Product type");
        ValidationUtil.validateMaxLength("Description", description, 2000);
    }
}

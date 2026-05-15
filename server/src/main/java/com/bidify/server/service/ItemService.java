package com.bidify.server.service;

import com.bidify.common.dto.ItemDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateItemRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.ItemImageLink;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ItemMapper;
import com.bidify.server.utility.ServiceUtil;

import java.util.ArrayList;
import java.util.List;

public class ItemService {
    private static final ItemService instance = new ItemService();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();
    private final ImageService imageService = ImageService.getInstance();

    private ItemService() {}

    public static ItemService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.CREATE_ITEM, this::create);
        router.register(RequestType.GET_MY_INVENTORY, this::getMyInventory);
    }

    public Response create(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            CreateItemRequest data = JsonUtil.fromMap(request.getData(), CreateItemRequest.class);
            ServiceUtil.validateRequestData(data);
            ServiceUtil.requireSession(client);

            String ownerUsername = data.getOwnerUsername();
            String currentUsername = client.getCurrentUsername();
            requireOwner(currentUsername, ownerUsername);
            validateItemFields(data);

            Item item = new Item(
                    ownerUsername,
                    data.getName().trim(),
                    data.getDescription().trim(),
                    trimToNull(data.getCategory()),
                    trimToNull(data.getProductType())
            );
            itemDao.create(item);

            List<String> images = data.getImagesBase64();
            if (images != null && !images.isEmpty()) {
                List<Image> savedImages = imageService.saveImages(images);
                for (Image image : savedImages)
                    imageDao.create(image);
                itemDao.saveItemImageLinks(item.getId(), savedImages);
            }

            ItemDto itemDto = toDtoWithThumbnail(item);
            return new Response(RequestStatus.SUCCESS, "Create item successfully", itemDto);
        });
    }

    public Response getMyInventory(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireSession(client);

            List<Item> items = itemDao.findByOwnerUsername(client.getCurrentUsername());
            List<ItemDto> inventory = new ArrayList<>();
            for (Item item : items)
                inventory.add(toDtoWithThumbnail(item));

            return new Response(RequestStatus.SUCCESS, "Inventory loaded successfully", inventory);
        });
    }

    private void requireOwner(String currentUsername, String ownerUsername) {
        ValidationUtil.validateUsername(currentUsername);
        ValidationUtil.validateUsername(ownerUsername);

        if (!currentUsername.equals(ownerUsername))
            throw new ValidationException("You do not have permission to create this item");
    }

    private void validateItemFields(CreateItemRequest data) {
        ValidationUtil.requiresNonBlank(data.getName(), "Item name");
        ValidationUtil.requiresNonBlank(data.getDescription(), "Description");
        ValidationUtil.requiresNonBlank(data.getCategory(), "Category");
        ValidationUtil.requiresNonBlank(data.getProductType(), "Product type");
        ValidationUtil.validateMaxLength("Description", data.getDescription(), 2000);
    }

    private ItemDto toDtoWithThumbnail(Item item) {
        ItemDto itemDto = ItemMapper.toDto(item);
        itemDto.setThumbnailBase64(getThumbnail(item.getId()));
        return itemDto;
    }

    private String getThumbnail(String itemId) {
        try {
            List<ItemImageLink> links = itemDao.getItemImageLinks(itemId);
            for (ItemImageLink link : links) {
                if (!link.isPrimary()) continue;
                Image image = imageDao.findById(link.getImageId());
                if (image != null)
                    return imageService.getBase64Image(image.getFilePath());
            }
            for (ItemImageLink link : links) {
                Image image = imageDao.findById(link.getImageId());
                if (image != null)
                    return imageService.getBase64Image(image.getFilePath());
            }
        }
        catch (DatabaseException ignored) {
            return null;
        }
        return null;
    }

    private String trimToNull(String value) {
        return value == null ? null : value.trim();
    }
}

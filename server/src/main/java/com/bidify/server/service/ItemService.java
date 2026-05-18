package com.bidify.server.service;

import com.bidify.common.dto.ItemDto;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateItemRequest;
import com.bidify.common.model.DeleteItemRequest;
import com.bidify.common.model.GetItemDetailRequest;
import com.bidify.common.model.GetUserInventoryRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdateItemRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.ItemImageLink;
import com.bidify.server.model.User;
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
        router.register(RequestType.GET_USER_INVENTORY, this::getUserInventory);
        router.register(RequestType.GET_ITEM_DETAIL, this::getItemDetail);
        router.register(RequestType.UPDATE_ITEM, this::update);
        router.register(RequestType.DELETE_ITEM, this::delete);
    }

    public Response create(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            CreateItemRequest data = JsonUtil.fromMap(request.getData(), CreateItemRequest.class);
            ServiceUtil.validateRequestData(data);

            User user = ServiceUtil.requireSessionUser(client);
            ServiceUtil.requireUserRole(user, "Admin accounts cannot create items");

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

            ItemDto itemDto = toDtoWithImages(item, false);
            return new Response(RequestStatus.SUCCESS, "Create item successfully", itemDto);
        });
    }

    public Response getMyInventory(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User user = ServiceUtil.requireSessionUser(client);
            return loadInventory(user.getUsername());
        });
    }

    public Response getUserInventory(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            GetUserInventoryRequest data = JsonUtil.fromMap(request.getData(), GetUserInventoryRequest.class);
            ServiceUtil.validateRequestData(data);
            ValidationUtil.validateUsername(data.getOwnerUsername());

            return loadInventory(data.getOwnerUsername());
        });
    }

    public Response getItemDetail(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            GetItemDetailRequest data = JsonUtil.fromMap(request.getData(), GetItemDetailRequest.class);
            ServiceUtil.validateRequestData(data);

            User user = ServiceUtil.requireSessionUser(client);

            Item item = requireAccessibleItem(user, data.getItemId(), "You do not have permission to view this item");
            return new Response(RequestStatus.SUCCESS, "Item loaded successfully", toDtoWithImages(item, true));
        });
    }

    public Response update(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            UpdateItemRequest data = JsonUtil.fromMap(request.getData(), UpdateItemRequest.class);
            ServiceUtil.validateRequestData(data);

            User user = ServiceUtil.requireSessionUser(client);
            validateItemFields(data.getName(), data.getDescription(), data.getCategory(), data.getProductType());

            Item item = requireAccessibleItem(user, data.getItemId(), "You do not have permission to edit this item");
            if (item.getAvailabilityStatus() != ItemStatus.AVAILABLE)
                throw new ValidationException("Only available items can be edited");

            item.setName(data.getName().trim());
            item.setDescription(data.getDescription().trim());
            item.setCategory(trimToNull(data.getCategory()));
            item.setProductType(trimToNull(data.getProductType()));
            itemDao.save(item);

            replaceItemImages(item.getId(), data.getImagesBase64());
            Item refreshed = itemDao.findById(item.getId());
            return new Response(RequestStatus.SUCCESS, "Item updated successfully", toDtoWithImages(refreshed, true));
        });
    }

    public Response delete(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            DeleteItemRequest data = JsonUtil.fromMap(request.getData(), DeleteItemRequest.class);
            ServiceUtil.validateRequestData(data);

            User user = ServiceUtil.requireSessionUser(client);
            Item item = requireAccessibleItem(user, data.getItemId(), "You do not have permission to delete this item");
            if (item.getAvailabilityStatus() != ItemStatus.AVAILABLE)
                throw new ValidationException("Only available items can be deleted");

            deleteItemAssets(item);
            itemDao.deleteById(item.getId());
            return new Response(RequestStatus.SUCCESS, "Item deleted successfully");
        });
    }

    private Response loadInventory(String ownerUsername) {
        List<Item> items = itemDao.findByOwnerUsername(ownerUsername);
        List<ItemDto> inventory = new ArrayList<>();
        for (Item item : items)
            inventory.add(toDtoWithImages(item, false));

        return new Response(RequestStatus.SUCCESS, "Inventory loaded successfully", inventory);
    }

    private void requireOwner(String currentUsername, String ownerUsername) {
        ValidationUtil.validateUsername(currentUsername);
        ValidationUtil.validateUsername(ownerUsername);

        if (!currentUsername.equals(ownerUsername))
            throw new ValidationException("You do not have permission to create this item");
    }

    private void validateItemFields(CreateItemRequest data) {
        validateItemFields(data.getName(), data.getDescription(), data.getCategory(), data.getProductType());
    }

    private void validateItemFields(String name, String description, String category, String productType) {
        ValidationUtil.requiresNonBlank(name, "Item name");
        ValidationUtil.requiresNonBlank(description, "Description");
        ValidationUtil.requiresNonBlank(category, "Category");
        ValidationUtil.requiresNonBlank(productType, "Product type");
        ValidationUtil.validateMaxLength("Description", description, 2000);
    }

    private Item requireAccessibleItem(User user, String itemId, String permissionMessage) throws DatabaseException {
        ValidationUtil.requiresNonBlank(itemId, "Item ID");

        Item item = itemDao.findById(itemId);
        if (item == null)
            throw new ValidationException("Item not found");
        if (!ServiceUtil.isOwnerOrAdmin(item.getOwnerUsername(), user.getUsername()))
            throw new ValidationException(permissionMessage);
        return item;
    }

    private ItemDto toDtoWithImages(Item item, boolean includeGallery) {
        ItemDto itemDto = ItemMapper.toDto(item);
        List<String> gallery = getGallery(item.getId());
        itemDto.setThumbnailBase64(gallery.isEmpty() ? null : gallery.get(0));
        if (includeGallery)
            itemDto.setGalleryBase64(gallery);
        return itemDto;
    }

    private List<String> getGallery(String itemId) {
        List<String> gallery = new ArrayList<>();
        try {
            for (ItemImageLink link : itemDao.getItemImageLinks(itemId)) {
                Image image = imageDao.findById(link.getImageId());
                if (image == null) continue;

                String base64 = imageService.getBase64Image(image.getFilePath());
                if (base64 != null && !base64.isBlank())
                    gallery.add(base64);
            }
        }
        catch (DatabaseException ignored) {
            return List.of();
        }
        return gallery;
    }

    private void replaceItemImages(String itemId, List<String> imagesBase64) throws DatabaseException {
        clearItemImages(itemId);

        if (imagesBase64 == null || imagesBase64.isEmpty())
            return;

        List<Image> savedImages = imageService.saveImages(imagesBase64);
        for (Image image : savedImages)
            imageDao.create(image);
        itemDao.saveItemImageLinks(itemId, savedImages);
    }

    private void deleteItemAssets(Item item) throws DatabaseException {
        clearItemImages(item.getId());
    }

    private void clearItemImages(String itemId) throws DatabaseException {
        for (String imageId : itemDao.findImageIdsByItemId(itemId)) {
            Image image = imageDao.findById(imageId);
            if (image != null)
                imageService.deleteImageFile(image.getFilePath());
            imageDao.deleteById(imageId);
        }
        itemDao.deleteItemImageLinks(itemId);
    }

    private String trimToNull(String value) {
        return value == null ? null : value.trim();
    }
}

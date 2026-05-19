package com.bidify.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.bidify.common.dto.ItemDto;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.model.ClientSession;
import com.bidify.service.InventoryClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class InventoryController {
    private static String managedOwnerUsername;

    private final InventoryClientService inventoryClientService = new InventoryClientService();
    private List<ItemDto> allItems = List.of();

    @FXML
    private TextField searchField;

    @FXML
    private VBox itemsContainer;

    @FXML
    private Label summaryLabel;

    @FXML
    private Button addItemButton;

    // dùng để thiết lập managed chủ sở hữu username
    public static void setManagedOwnerUsername(String ownerUsername) {
        managedOwnerUsername = ownerUsername;
    }

    // dùng để khởi tạo
    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            MissionBarUtil.setup(NavPage.INVENTORY, false, null);
            searchField.textProperty().addListener((obs, oldValue, newValue) -> renderItems(filterItems(newValue)));
            if (addItemButton != null) {
                boolean allowCreate = !ClientSession.getInstance().isAdmin() && !isAdminManagedView();
                addItemButton.setManaged(allowCreate);
                addItemButton.setVisible(allowCreate);
            }
            // dùng để tải kho đồ
            loadInventory();
        });
    }

    // dùng để xử lý thêm sản phẩm
    @FXML
    private void handleAddItem() {
        if (ClientSession.getInstance().isAdmin()) {
            NotificationUtil.info("Admin accounts cannot create items.");
            return;
        }
        ItemDetailController.setItemId(null);
        SceneManager.clearCache("item-detail.fxml");
        SceneManager.switchScene("item-detail.fxml", false, false);
    }

    // dùng để xử lý refresh
    @FXML
    private void handleRefresh() {
        // dùng để tải kho đồ
        loadInventory();
    }

    // dùng để tải kho đồ
    private void loadInventory() {
        try {
            if (isAdminManagedView()) {
                allItems = inventoryClientService.getInventoryForOwner(managedOwnerUsername);
            }
            else {
                allItems = inventoryClientService.getMyInventory();
            }
            renderItems(filterItems(searchField.getText()));
        }
        catch (IOException e) {
            allItems = List.of();
            renderItems(List.of());
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            allItems = List.of();
            renderItems(List.of());
            NotificationUtil.error(e.getMessage());
        }
    }

    // dùng để filter danh sách sản phẩm
    private List<ItemDto> filterItems(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase();
        if (query.isBlank()) return allItems;

        List<ItemDto> filtered = new ArrayList<>();
        for (ItemDto item : allItems) {
            if (item == null) continue;

            String haystack = String.join(" ",
                safe(item.getName()),
                safe(item.getDescription()),
                safe(item.getCategory()),
                safe(item.getProductType()),
                safe(item.getAvailabilityStatus())
            ).toLowerCase();

            if (haystack.contains(query))
                filtered.add(item);
        }
        return filtered;
    }

    // dùng để hiển thị danh sách sản phẩm
    private void renderItems(List<ItemDto> items) {
        itemsContainer.getChildren().clear();

        if (items == null || items.isEmpty()) {
            itemsContainer.getChildren().add(createEmptyState());
            summaryLabel.setText(allItems.isEmpty() ? "No inventory items yet" : "No items match your search");
            return;
        }

        for (int i = 0; i < items.size(); i++)
            itemsContainer.getChildren().add(createRow(items.get(i), i == items.size() - 1));

        summaryLabel.setText("Showing " + items.size() + " item" + (items.size() == 1 ? "" : "s"));
    }

    // dùng để tạo empty state
    private Node createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(28, 32, 28, 32));

        Label title = new Label(allItems.isEmpty() ? "Your inventory is empty." : "No items match that search.");
        title.getStyleClass().add("empty-state-title");

        Label subtitle = new Label(allItems.isEmpty()
            ? "Create an item first, then you can list it in an auction."
            : "Try a different keyword or clear the search field.");
        subtitle.getStyleClass().add("empty-state-subtitle");
        subtitle.setWrapText(true);

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    // dùng để tạo dòng hiển thị
    private Node createRow(ItemDto item, boolean isLast) {
        GridPane row = new GridPane();
        row.getStyleClass().add(isLast ? "table-row-last" : "table-row");
        row.getColumnConstraints().addAll(createColumn(70), createColumn(30));
        row.setPadding(new Insets(24, 32, 24, 32));

        HBox details = new HBox(16);
        details.setAlignment(Pos.TOP_LEFT);
        details.getChildren().add(createThumbnailNode(item));

        VBox textBox = new VBox(6);
        Label title = new Label(defaultText(item.getName(), "Unnamed item"));
        title.getStyleClass().add("item-title");

        Label description = new Label(defaultText(item.getDescription(), "No description."));
        description.getStyleClass().add("item-description");
        description.setWrapText(true);
        description.setMaxWidth(560);

        Label meta = new Label(buildMetaLine(item));
        meta.getStyleClass().add("sku-text");
        textBox.getChildren().addAll(title, description, meta);

        details.getChildren().add(textBox);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);

        Label statusChip = new Label(formatStatus(item.getAvailabilityStatus()));
        statusChip.getStyleClass().addAll(
            "status-chip",
            ItemStatus.LOCKED_IN_AUCTION.name().equals(item.getAvailabilityStatus()) ? "status-chip-locked" : "status-chip-available"
        );

        Button detailsButton = new Button("Edit");
        detailsButton.getStyleClass().add("outline-button");
        boolean editable = ItemStatus.AVAILABLE.name().equals(item.getAvailabilityStatus());
        detailsButton.setDisable(!editable);
        detailsButton.setOnAction(event -> {
            if (!editable) {
                NotificationUtil.info("Only available items can be edited.");
                return;
            }
            openItemEditor(item.getId());
        });

        actions.getChildren().add(statusChip);
        if (isAdminManagedView()) {
            Button deleteButton = new Button("Delete");
            deleteButton.getStyleClass().add("outline-button");
            deleteButton.setDisable(!editable);
            deleteButton.setOnAction(event -> handleDeleteItem(item, editable));
            actions.getChildren().add(deleteButton);
        }
        actions.getChildren().add(detailsButton);

        row.add(details, 0, 0);
        row.add(actions, 1, 0);
        return row;
    }

    // dùng để tạo column
    private ColumnConstraints createColumn(double percentWidth) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(percentWidth);
        return constraints;
    }

    // dùng để mở sản phẩm editor
    private void openItemEditor(String itemId) {
        ItemDetailController.setItemId(itemId);
        SceneManager.clearCache("item-detail.fxml");
        SceneManager.switchScene("item-detail.fxml", false, false);
    }

    // dùng để xử lý xóa sản phẩm
    private void handleDeleteItem(ItemDto item, boolean editable) {
        if (!editable) {
            NotificationUtil.info("Only available items can be deleted.");
            return;
        }

        try {
            inventoryClientService.deleteItem(item.getId());
            NotificationUtil.success("Item deleted successfully.");
            // dùng để tải kho đồ
            loadInventory();
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    // dùng để tạo thumbnail node
    private Node createThumbnailNode(ItemDto item) {
        StackPane frame = new StackPane();
        frame.getStyleClass().add("thumb-frame");
        frame.setPrefSize(64, 64);

        Image image = decodeImage(item.getThumbnailBase64());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(64);
            imageView.setFitHeight(64);
            imageView.setPreserveRatio(false);
            imageView.getStyleClass().add("thumb");
            frame.getChildren().add(imageView);
        }
        else {
            Label placeholder = new Label(resolveInitial(item.getName()));
            placeholder.getStyleClass().add("thumb-placeholder");
            frame.getChildren().add(placeholder);
        }

        return frame;
    }

    // dùng để decode hình ảnh
    private Image decodeImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            return new Image(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    // dùng để build meta line
    private String buildMetaLine(ItemDto item) {
        List<String> parts = new ArrayList<>();
        if (!safe(item.getCategory()).isBlank())
            parts.add(item.getCategory());
        if (!safe(item.getProductType()).isBlank())
            parts.add(item.getProductType());
        parts.add("Status: " + formatStatus(item.getAvailabilityStatus()));
        return String.join(" • ", parts);
    }

    // dùng để giải quyết initial
    private String resolveInitial(String value) {
        if (value == null || value.isBlank()) return "I";
        return value.substring(0, 1).toUpperCase();
    }

    // dùng để định dạng trạng thái
    private String formatStatus(String availabilityStatus) {
        if (ItemStatus.LOCKED_IN_AUCTION.name().equals(availabilityStatus))
            return "Locked";
        return "Available";
    }

    // dùng để default text
    private String defaultText(String value, String fallback) {
        return safe(value).isBlank() ? fallback : value;
    }

    // dùng để safe
    private String safe(String value) {
        return value == null ? "" : value;
    }

    // dùng để kiểm tra xem quản trị viên (admin) managed giao diện
    private boolean isAdminManagedView() {
        return managedOwnerUsername != null && !managedOwnerUsername.isBlank();
    }
}

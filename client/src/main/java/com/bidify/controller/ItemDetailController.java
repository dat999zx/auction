package com.bidify.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.bidify.common.dto.ItemDto;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.utility.ImageUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.service.InventoryClientService;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

public class ItemDetailController {
    private static final int MAX_IMAGES = 10;
    private static String currentItemId;

    private final InventoryClientService inventoryClientService = new InventoryClientService();
    private final List<GalleryImageEntry> galleryEntries = new ArrayList<>();

    @FXML
    private TextField productNameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private ComboBox<String> productTypeComboBox;

    @FXML
    private ImageView primaryImageView;

    @FXML
    private FlowPane galleryPreviewPane;

    @FXML
    private Label statusFooterLabel;

    // dùng để thiết lập sản phẩm ID
    public static void setItemId(String itemId) {
        currentItemId = itemId;
    }

    // dùng để khởi tạo
    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            categoryComboBox.getItems().setAll("Electronics", "Fashion", "Art", "Collectibles", "Vehicles", "Other");
            productTypeComboBox.getItems().setAll("New", "Used", "Rare", "Vintage", "Limited");
            // dùng để hiển thị gallery
            renderGallery();

            if (currentItemId == null || currentItemId.isBlank()) {
                statusFooterLabel.setText("New item draft");
            } else {
                // dùng để tải sản phẩm cho edit
                loadItemForEdit(currentItemId);
            }
        });
    }

    // dùng để xử lý back
    @FXML
    private void handleBack() {
        // dùng để xóa sạch editing state
        clearEditingState();
        SceneManager.switchScene("inventory.fxml", false, true);
    }

    // dùng để xử lý hủy bỏ
    @FXML
    private void handleCancel() {
        // dùng để xóa sạch editing state
        clearEditingState();
        SceneManager.switchScene("inventory.fxml", false, true);
    }

    // dùng để xử lý pick images
    @FXML
    private void handlePickImages(MouseEvent event) {
        // dùng để mở chooser
        openChooser();
    }

    // dùng để xử lý pick images
    @FXML
    private void handlePickImages() {
        // dùng để mở chooser
        openChooser();
    }

    // dùng để xử lý lưu
    @FXML
    private void handleSave() {
        try {
            // dùng để kiểm tra tính hợp lệ fields
            validateFields();
            List<String> encodedImages = encodeGalleryImages();

            ItemDto savedItem;
            if (currentItemId == null || currentItemId.isBlank()) {
                savedItem = inventoryClientService.createItem(
                    productNameField.getText().trim(),
                    descriptionArea.getText().trim(),
                    categoryComboBox.getValue(),
                    productTypeComboBox.getValue(),
                    encodedImages
                );
                NotificationUtil.success("Item created successfully.");
                statusFooterLabel.setText("Created item: " + savedItem.getId());
            }
            else {
                savedItem = inventoryClientService.updateItem(
                    currentItemId,
                    productNameField.getText().trim(),
                    descriptionArea.getText().trim(),
                    categoryComboBox.getValue(),
                    productTypeComboBox.getValue(),
                    encodedImages
                );
                NotificationUtil.success("Item updated successfully.");
                statusFooterLabel.setText("Updated item: " + savedItem.getId());
            }

            // dùng để xóa sạch editing state
            clearEditingState();
            SceneManager.clearCache("inventory.fxml");
            SceneManager.switchScene("inventory.fxml", false, true);
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    // dùng để tải sản phẩm cho edit
    private void loadItemForEdit(String itemId) {
        Thread loader = new Thread(() -> {
            try {
                ItemDto item = inventoryClientService.getItemDetail(itemId);
                Platform.runLater(() -> bindItem(item));
            }
            catch (IOException e) {
                Platform.runLater(() -> NotificationUtil.error("Cannot connect to server."));
            }
            catch (ValidationException e) {
                Platform.runLater(() -> NotificationUtil.error(e.getMessage()));
            }
        });
        loader.setDaemon(true);
        loader.start();
    }

    // dùng để liên kết dữ liệu sản phẩm
    private void bindItem(ItemDto item) {
        productNameField.setText(safe(item.getName()));
        descriptionArea.setText(safe(item.getDescription()));
        categoryComboBox.setValue(item.getCategory());
        productTypeComboBox.setValue(item.getProductType());

        galleryEntries.clear();
        if (item.getGalleryBase64() != null) {
            for (String base64 : item.getGalleryBase64()) {
                if (base64 != null && !base64.isBlank())
                    galleryEntries.add(fromBase64(base64));
            }
        }

        statusFooterLabel.setText("Editing item: " + item.getId());
        // dùng để hiển thị gallery
        renderGallery();
    }

    // dùng để mở chooser
    private void openChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Item Images");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        List<File> files = chooser.showOpenMultipleDialog(
            primaryImageView != null && primaryImageView.getScene() != null ? primaryImageView.getScene().getWindow() : null
        );
        if (files == null || files.isEmpty()) return;

        for (File file : files) {
            if (galleryEntries.size() >= MAX_IMAGES) {
                NotificationUtil.error("Maximum " + MAX_IMAGES + " images allowed.");
                break;
            }
            if (!containsFile(file))
                galleryEntries.add(fromFile(file));
        }

        // dùng để hiển thị gallery
        renderGallery();
    }

    // dùng để contains file
    private boolean containsFile(File candidate) {
        String target = candidate.getAbsolutePath();
        for (GalleryImageEntry entry : galleryEntries) {
            if (entry.file != null && target.equals(entry.file.getAbsolutePath()))
                return true;
        }
        return false;
    }

    // dùng để hiển thị gallery
    private void renderGallery() {
        galleryPreviewPane.getChildren().clear();

        if (galleryEntries.isEmpty()) {
            primaryImageView.setImage(null);
            statusFooterLabel.setText(currentItemId == null || currentItemId.isBlank() ? "New item draft" : "Editing item: " + currentItemId);
            Label empty = new Label("No images selected yet");
            empty.getStyleClass().add("helper-text");
            galleryPreviewPane.getChildren().add(empty);
            return;
        }

        primaryImageView.setImage(galleryEntries.get(0).toImage());
        statusFooterLabel.setText((currentItemId == null || currentItemId.isBlank() ? "Selected " : "Editing " )
            + galleryEntries.size() + " image" + (galleryEntries.size() == 1 ? "" : "s")
            + ". Click a thumbnail to make it primary.");

        for (GalleryImageEntry entry : galleryEntries)
            galleryPreviewPane.getChildren().add(createPreviewTile(entry));
    }

    // dùng để tạo preview tile
    private StackPane createPreviewTile(GalleryImageEntry entry) {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("media-preview-tile");
        tile.setPrefSize(112, 112);

        ImageView imageView = new ImageView(entry.toImage());
        imageView.setFitWidth(112);
        imageView.setFitHeight(112);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("secondary-thumb");

        Button removeButton = new Button("x");
        removeButton.getStyleClass().add("remove-image-button");
        StackPane.setAlignment(removeButton, Pos.TOP_RIGHT);
        removeButton.setOnAction(event -> {
            galleryEntries.remove(entry);
            // dùng để hiển thị gallery
            renderGallery();
        });

        tile.setOnMouseClicked(event -> {
            if (!galleryEntries.isEmpty() && galleryEntries.get(0) != entry) {
                galleryEntries.remove(entry);
                galleryEntries.add(0, entry);
                // dùng để hiển thị gallery
                renderGallery();
            }
        });

        tile.getChildren().addAll(imageView, removeButton);
        return tile;
    }

    // dùng để encode gallery images
    private List<String> encodeGalleryImages() throws IOException {
        List<String> imagesBase64 = new ArrayList<>();
        for (GalleryImageEntry entry : galleryEntries)
            imagesBase64.add(entry.toBase64());
        return imagesBase64;
    }

    // dùng để kiểm tra tính hợp lệ fields
    private void validateFields() {
        ValidationUtil.requiresNonBlank(productNameField.getText(), "Item name");
        ValidationUtil.requiresNonBlank(descriptionArea.getText(), "Description");

        if (categoryComboBox.getValue() == null)
            throw new ValidationException("Please select a category");
        if (productTypeComboBox.getValue() == null)
            throw new ValidationException("Please select a product type");
    }

    // dùng để decode base64image
    private Image decodeBase64Image(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            return new Image(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    // dùng để safe
    private String safe(String value) {
        return value == null ? "" : value;
    }

    // dùng để xóa sạch editing state
    private void clearEditingState() {
        currentItemId = null;
        galleryEntries.clear();
    }

    // dùng để từ file
    private GalleryImageEntry fromFile(File file) {
        return new GalleryImageEntry(file, null);
    }

    // dùng để từ base64
    private GalleryImageEntry fromBase64(String base64) {
        return new GalleryImageEntry(null, base64);
    }

    private final class GalleryImageEntry {
        private final File file;
        private final String base64;

        // dùng để gallery hình ảnh entry
        private GalleryImageEntry(File file, String base64) {
            this.file = file;
            this.base64 = base64;
        }

        // dùng để chuyển thành hình ảnh
        private Image toImage() {
            if (file != null)
                return new Image(file.toURI().toString());
            // dùng để decode base64image
            return decodeBase64Image(base64);
        }

        // dùng để chuyển thành base64
        private String toBase64() throws IOException {
            if (base64 != null)
                return base64;

            byte[] fileContent = Files.readAllBytes(file.toPath());
            byte[] resizedContent = ImageUtil.resizeImage(fileContent, 800);
            return Base64.getEncoder().encodeToString(resizedContent);
        }
    }
}

package com.bidify.controller;

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
import com.bidify.media.ImageCache;
import com.bidify.ui.NotificationUtil;
import com.bidify.navigation.SceneManager;

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
    private static final int MAX_IMAGES = 5;
    private static final int MAX_IMAGE_DIMENSION = 1200;
    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
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

    @FXML
    private Button saveButton;

    public static void setItemId(String itemId) {
        currentItemId = itemId;
    }

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            categoryComboBox.getItems().setAll("Electronics", "Fashion", "Art", "Collectibles", "Vehicles", "Other");
            productTypeComboBox.getItems().setAll("New", "Used", "Rare", "Vintage", "Limited");
            renderGallery();

            if (currentItemId == null || currentItemId.isBlank()) {
                statusFooterLabel.setText("New item draft");
            } else {
                loadItemForEdit(currentItemId);
            }
        });
    }

    @FXML
    private void handleBack() {
        clearEditingState();
        SceneManager.goInventory();
    }

    @FXML
    private void handleCancel() {
        clearEditingState();
        SceneManager.goInventory();
    }

    @FXML
    private void handlePickImages(MouseEvent event) {
        openChooser();
    }

    @FXML
    private void handlePickImages() {
        openChooser();
    }

    @FXML
    private void handleSave() {
        try {
            validateFields();
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
            return;
        }

        String itemId = currentItemId;
        String name = productNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String category = categoryComboBox.getValue();
        String productType = productTypeComboBox.getValue();
        List<GalleryImageEntry> entriesToSave = new ArrayList<>(galleryEntries);

        setSavingState(true);

        Thread saver = new Thread(() -> {
            try {
                List<String> encodedImages = encodeGalleryImages(entriesToSave);
                ItemDto savedItem = saveItem(itemId, name, description, category, productType, encodedImages);
                Platform.runLater(() -> handleSaveSuccess(savedItem, itemId));
            }
            catch (IOException e) {
                Platform.runLater(() -> {
                    setSavingState(false);
                    NotificationUtil.error("Cannot connect to server.");
                });
            }
            catch (ValidationException e) {
                Platform.runLater(() -> {
                    setSavingState(false);
                    NotificationUtil.error(e.getMessage());
                });
            }
        });
        saver.setDaemon(true);
        saver.start();
    }

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
        renderGallery();
    }

    private void openChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Item Images");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
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
            if (file.length() > MAX_IMAGE_BYTES) {
                NotificationUtil.error("Image files must be 10 MB or smaller.");
                continue;
            }
            if (!containsFile(file))
                galleryEntries.add(fromFile(file));
        }

        renderGallery();
    }

    private boolean containsFile(File candidate) {
        String target = candidate.getAbsolutePath();
        for (GalleryImageEntry entry : galleryEntries) {
            if (entry.file != null && target.equals(entry.file.getAbsolutePath()))
                return true;
        }
        return false;
    }

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

    private StackPane createPreviewTile(GalleryImageEntry entry) {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("media-preview-tile");
        tile.setPrefSize(112, 112);

        ImageView imageView = new ImageView(entry.toImage());
        imageView.setFitWidth(112);
        imageView.setFitHeight(112);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("secondary-thumb");

        Button removeButton = new Button("x");
        removeButton.getStyleClass().add("remove-image-button");
        StackPane.setAlignment(removeButton, Pos.TOP_RIGHT);
        removeButton.setOnAction(event -> {
            galleryEntries.remove(entry);
            renderGallery();
        });

        tile.setOnMouseClicked(event -> {
            if (!galleryEntries.isEmpty() && galleryEntries.get(0) != entry) {
                galleryEntries.remove(entry);
                galleryEntries.add(0, entry);
                renderGallery();
            }
        });

        tile.getChildren().addAll(imageView, removeButton);
        return tile;
    }

    private ItemDto saveItem(String itemId, String name, String description, String category, String productType,
            List<String> encodedImages) throws IOException {
        if (itemId == null || itemId.isBlank()) {
            return inventoryClientService.createItem(
                name,
                description,
                category,
                productType,
                encodedImages
            );
        }

        return inventoryClientService.updateItem(
            itemId,
            name,
            description,
            category,
            productType,
            encodedImages
        );
    }

    private void handleSaveSuccess(ItemDto savedItem, String savedItemId) {
        NotificationUtil.success(savedItemId == null || savedItemId.isBlank()
            ? "Item created successfully."
            : "Item updated successfully.");
        statusFooterLabel.setText((savedItemId == null || savedItemId.isBlank() ? "Created item: " : "Updated item: ")
            + savedItem.getId());
        setSavingState(false);
        clearEditingState();
        SceneManager.goInventory();
    }

    private void setSavingState(boolean saving) {
        if (saveButton != null)
            saveButton.setDisable(saving);
        statusFooterLabel.setText(saving ? "Saving item media..." : "New item draft");
    }

    private List<String> encodeGalleryImages(List<GalleryImageEntry> entries) throws IOException {
        List<String> imagesBase64 = new ArrayList<>();
        for (GalleryImageEntry entry : entries)
            imagesBase64.add(entry.toBase64());
        return imagesBase64;
    }

    private void validateFields() {
        ValidationUtil.requiresNonBlank(productNameField.getText(), "Item name");
        ValidationUtil.requiresNonBlank(descriptionArea.getText(), "Description");

        if (categoryComboBox.getValue() == null)
            throw new ValidationException("Please select a category");
        if (productTypeComboBox.getValue() == null)
            throw new ValidationException("Please select a product type");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void clearEditingState() {
        currentItemId = null;
        galleryEntries.clear();
    }

    private GalleryImageEntry fromFile(File file) {
        return new GalleryImageEntry(file, null);
    }

    private GalleryImageEntry fromBase64(String base64) {
        return new GalleryImageEntry(null, base64);
    }

    private final class GalleryImageEntry {
        private final File file;
        private final String base64;
        private Image cachedImage;

        private GalleryImageEntry(File file, String base64) {
            this.file = file;
            this.base64 = base64;
        }

        private Image toImage() {
            try {
                Image img = null;
                if (file != null) {
                    if (cachedImage == null)
                        cachedImage = new Image(file.toURI().toString(), 360, 360, true, true, true);
                    img = cachedImage;
                } else {
                    img = ImageCache.decode(base64);
                }
                if (img != null && img.isError()) {
                    return null;
                }
                return img;
            } catch (Exception e) {
                return null;
            }
        }

        private String toBase64() throws IOException {
            if (base64 != null)
                return base64;

            byte[] fileContent = Files.readAllBytes(file.toPath());
            byte[] resizedContent = ImageUtil.resizeImage(fileContent, MAX_IMAGE_DIMENSION);
            return Base64.getEncoder().encodeToString(resizedContent);
        }
    }
}

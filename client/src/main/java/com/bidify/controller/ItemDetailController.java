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

    private final InventoryClientService inventoryClientService = new InventoryClientService();
    private final List<File> selectedImageFiles = new ArrayList<>();

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
    private void initialize() {
        Platform.runLater(() -> {
            categoryComboBox.getItems().setAll("Electronics", "Fashion", "Art", "Collectibles", "Vehicles", "Other");
            productTypeComboBox.getItems().setAll("New", "Used", "Rare", "Vintage", "Limited");
            statusFooterLabel.setText("New item draft");
            renderGallery();
        });
    }

    @FXML
    private void handleBack() {
        SceneManager.switchScene("inventory.fxml", false, true);
    }

    @FXML
    private void handleCancel() {
        SceneManager.switchScene("inventory.fxml", false, true);
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
            ItemDto createdItem = inventoryClientService.createItem(
                productNameField.getText().trim(),
                descriptionArea.getText().trim(),
                categoryComboBox.getValue(),
                productTypeComboBox.getValue(),
                encodeSelectedImages()
            );
            NotificationUtil.success("Item created successfully.");
            statusFooterLabel.setText("Created item: " + createdItem.getId());
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
            if (selectedImageFiles.size() >= MAX_IMAGES) {
                NotificationUtil.error("Maximum " + MAX_IMAGES + " images allowed.");
                break;
            }
            if (!selectedImageFiles.contains(file))
                selectedImageFiles.add(file);
        }

        renderGallery();
    }

    private void renderGallery() {
        galleryPreviewPane.getChildren().clear();

        if (selectedImageFiles.isEmpty()) {
            primaryImageView.setImage(null);
            statusFooterLabel.setText("New item draft");
            Label empty = new Label("No images selected yet");
            empty.getStyleClass().add("helper-text");
            galleryPreviewPane.getChildren().add(empty);
            return;
        }

        primaryImageView.setImage(new Image(selectedImageFiles.get(0).toURI().toString()));
        statusFooterLabel.setText("Selected " + selectedImageFiles.size() + " image" + (selectedImageFiles.size() == 1 ? "" : "s"));

        for (File file : selectedImageFiles)
            galleryPreviewPane.getChildren().add(createPreviewTile(file));
    }

    private StackPane createPreviewTile(File file) {
        StackPane tile = new StackPane();
        tile.getStyleClass().add("media-preview-tile");
        tile.setPrefSize(112, 112);

        ImageView imageView = new ImageView(new Image(file.toURI().toString()));
        imageView.setFitWidth(112);
        imageView.setFitHeight(112);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("secondary-thumb");

        Button removeButton = new Button("x");
        removeButton.getStyleClass().add("remove-image-button");
        StackPane.setAlignment(removeButton, Pos.TOP_RIGHT);
        removeButton.setOnAction(event -> {
            selectedImageFiles.remove(file);
            renderGallery();
        });

        tile.getChildren().addAll(imageView, removeButton);
        return tile;
    }

    private List<String> encodeSelectedImages() throws IOException {
        List<String> imagesBase64 = new ArrayList<>();
        for (File file : selectedImageFiles) {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            byte[] resizedContent = ImageUtil.resizeImage(fileContent, 800);
            imagesBase64.add(Base64.getEncoder().encodeToString(resizedContent));
        }
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
}

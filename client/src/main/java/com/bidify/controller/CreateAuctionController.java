package com.bidify.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.network.SocketClient;
import com.bidify.service.AuctionClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

public class CreateAuctionController {
    private static final Logger logger = LoggerFactory.getLogger(CreateAuctionController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_IMAGES = 10;

    @FXML
    private TextField productNameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private ComboBox<String> productTypeComboBox;

    @FXML
    private TextField startingPriceField;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField startTimeField;

    @FXML
    private TextField minIncrementField;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private TextField endTimeField;

    @FXML
    private StackPane uploadBox;

    @FXML
    private FlowPane imagePreviewPane;

    private final List<File> selectedImageFiles = new ArrayList<>();

    private final AuctionClientService auctionClientService = new AuctionClientService();

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            bindTopBar();

            if (categoryComboBox != null) {
                categoryComboBox.getItems().setAll(
                    List.of("Electronics", "Fashion", "Art", "Collectibles", "Vehicles", "Other")
                );
            }

            if (productTypeComboBox != null) {
                productTypeComboBox.getItems().setAll(
                    List.of("New", "Used", "Rare", "Vintage", "Limited")
                );
            }

            if (startDatePicker != null) {
                startDatePicker.setEditable(false);
                startDatePicker.setValue(LocalDate.now());
            }

            if (endDatePicker != null) {
                endDatePicker.setEditable(false);
                endDatePicker.setValue(LocalDate.now().plusDays(7));
            }

            if (startTimeField != null) {
                startTimeField.setText("09:00");
            }

            if (endTimeField != null) {
                endTimeField.setText("18:00");
            }
        });
    }

    @FXML
    private void handleUploadClick(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Images");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(uploadBox.getScene().getWindow());
        if (files != null) {
            for (File file : files) {
                if (selectedImageFiles.size() >= MAX_IMAGES) {
                    NotificationUtil.error("Maximum " + MAX_IMAGES + " images allowed.");
                    break;
                }
                if (!selectedImageFiles.contains(file)) {
                    selectedImageFiles.add(file);
                }
            }
            renderImagePreviews();
        }
    }

    private void renderImagePreviews() {
        imagePreviewPane.getChildren().clear();
        for (File file : selectedImageFiles) {
            StackPane tile = new StackPane();
            tile.getStyleClass().add("preview-tile");
            tile.setPrefSize(84, 84);

            ImageView imageView = new ImageView(new Image(file.toURI().toString()));
            imageView.setFitWidth(84);
            imageView.setFitHeight(84);
            imageView.setPreserveRatio(true);

            Button removeBtn = new Button("x");
            removeBtn.getStyleClass().add("remove-image-button");
            StackPane.setAlignment(removeBtn, Pos.TOP_RIGHT);
            removeBtn.setOnAction(e -> {
                selectedImageFiles.remove(file);
                renderImagePreviews();
            });

            tile.getChildren().addAll(imageView, removeBtn);

            // Add "Primary" label for the first image
            if (selectedImageFiles.indexOf(file) == 0) {
                Label primaryLabel = new Label("PRIMARY");
                primaryLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: white; -fx-font-size: 9px; -fx-padding: 2px 4px;");
                StackPane.setAlignment(primaryLabel, Pos.BOTTOM_LEFT);
                tile.getChildren().add(primaryLabel);
            }

            imagePreviewPane.getChildren().add(tile);
        }
    }

    @FXML
    private void createAuction() {
        try {
            validateInputs();

            String productName = productNameField.getText().trim();
            String description = descriptionArea.getText().trim();
            String category = categoryComboBox.getValue();
            String productType = productTypeComboBox.getValue();
            double startingPrice = parseAmount(startingPriceField.getText(), "Starting price");
            double minIncrement = parseAmount(minIncrementField.getText(), "Min increment");
            
            LocalDate startDate = startDatePicker.getValue();
            LocalTime startTime = parseTime(startTimeField.getText(), "Start time");
            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);

            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = parseTime(endTimeField.getText(), "End time");
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

            if (startDateTime.isBefore(LocalDateTime.now().minusMinutes(1))) {
                throw new ValidationException("Start time cannot be in the past");
            }

            if (endDateTime.isBefore(startDateTime.plusHours(1))) {
                throw new ValidationException("End time must be at least 1 hour after start time");
            }

            // convert images to Base64
            List<String> imagesBase64 = new ArrayList<>();
            for (File file : selectedImageFiles) {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                imagesBase64.add(Base64.getEncoder().encodeToString(fileContent));
            }

            CreateAuctionRequest data = new CreateAuctionRequest(
                com.bidify.network.SocketClient.getClient().getCurrentUsername(),
                productName,
                description,
                category,
                productType,
                startingPrice,
                minIncrement,
                startDateTime.toString(),
                endDateTime.toString(),
                imagesBase64
            );

            Response response = auctionClientService.createAuction(data);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Auction created successfully");
                SceneManager.clearCache("create-auction.fxml");
                SceneManager.switchScene("hub.fxml", false, true);
            } else {
                NotificationUtil.error(response.getMessage());
            }
        }
        catch (AuctionException | ValidationException | NumberFormatException e) {
            NotificationUtil.error(e.getMessage());
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server or process images");
            logger.error("Exception occurred", e);
        }
    }

    private void validateInputs() {
        ValidationUtil.requiresNonBlank(productNameField.getText(), "Product name");
        ValidationUtil.requiresNonBlank(descriptionArea.getText(), "Description");
        
        if (categoryComboBox.getValue() == null) {
            throw new ValidationException("Please select a category");
        }
        
        if (productTypeComboBox.getValue() == null) {
            throw new ValidationException("Please select a product type");
        }

        if (startDatePicker.getValue() == null) {
            throw new ValidationException("Please select a start date");
        }

        if (endDatePicker.getValue() == null) {
            throw new ValidationException("Please select an end date");
        }
    }

    private double parseAmount(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);

        try {
            double amount = Double.parseDouble(parseValue);
            if (amount < 0) throw new ValidationException(fieldName + " cannot be negative");
            return amount;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number");
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);

        try {
            return LocalTime.parse(parseValue, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must use HH:mm format (e.g., 09:30)");
        }
    }

    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.CREATE_AUCTION, false, null);
    }
}

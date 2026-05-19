package com.bidify.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.ItemDto;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.service.AuctionClientService;
import com.bidify.service.InventoryClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.StringConverter;

public class CreateAuctionController {
    private static final Logger logger = LoggerFactory.getLogger(CreateAuctionController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final InventoryClientService inventoryClientService = new InventoryClientService();

    @FXML
    private ComboBox<ItemDto> inventoryItemComboBox;

    @FXML
    private Label selectedItemNameLabel;

    @FXML
    private Label selectedDescriptionLabel;

    @FXML
    private Label selectedCategoryLabel;

    @FXML
    private Label selectedProductTypeLabel;

    @FXML
    private ImageView selectedItemImageView;

    @FXML
    private Label selectedItemHintLabel;

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

    // dùng để khởi tạo
    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            // dùng để liên kết dữ liệu top bar
            bindTopBar();
            // dùng để configure kho đồ selection
            configureInventorySelection();
            // dùng để tải kho đồ danh sách sản phẩm
            loadInventoryItems();

            startDatePicker.setEditable(false);
            startDatePicker.setValue(LocalDate.now());
            endDatePicker.setEditable(false);
            endDatePicker.setValue(LocalDate.now().plusDays(7));
            startTimeField.setText("09:00");
            endTimeField.setText("18:00");
        });
    }

    // dùng để tạo đấu giá
    @FXML
    private void createAuction() {
        try {
            // dùng để kiểm tra tính hợp lệ inputs
            validateInputs();

            ItemDto selectedItem = inventoryItemComboBox.getValue();
            double startingPrice = parseAmount(startingPriceField.getText(), "Starting price");
            double minIncrement = parseAmount(minIncrementField.getText(), "Min increment");

            LocalDate startDate = startDatePicker.getValue();
            LocalTime startTime = parseTime(startTimeField.getText(), "Start time");
            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);

            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = parseTime(endTimeField.getText(), "End time");
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

            if (startDateTime.isBefore(LocalDateTime.now().minusMinutes(1))) {
                startDateTime = LocalDateTime.now();
                startDatePicker.setValue(startDateTime.toLocalDate());
                startTimeField.setText(startDateTime.toLocalTime().format(TIME_FORMATTER));
            }
            
            CreateAuctionRequest data = new CreateAuctionRequest(
                com.bidify.network.SocketClient.getClient().getCurrentUsername(),
                selectedItem.getId(),
                startingPrice,
                minIncrement,
                startDateTime.toString(),
                endDateTime.toString()
            );

            Response response = auctionClientService.createAuction(data);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Auction created successfully");
                SceneManager.clearCache("create-auction.fxml");
                SceneManager.clearCache("inventory.fxml");
                SceneManager.switchScene("hub.fxml", false, true);
            }
            else {
                NotificationUtil.error(response.getMessage());
            }
        }
        catch (AuctionException | ValidationException | NumberFormatException e) {
            NotificationUtil.error(e.getMessage());
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        }
    }

    // dùng để configure kho đồ selection
    private void configureInventorySelection() {
        inventoryItemComboBox.setConverter(new StringConverter<>() {
            // dùng để chuyển thành string
            @Override
            public String toString(ItemDto item) {
                if (item == null) return "";
                return item.getName() + " • " + safe(item.getCategory()) + " • " + safe(item.getProductType());
            }

            // dùng để từ string
            @Override
            public ItemDto fromString(String string) {
                return null;
            }
        });

        inventoryItemComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateSelectedItemPreview(newValue));
    }

    // dùng để tải kho đồ danh sách sản phẩm
    private void loadInventoryItems() {
        try {
            List<ItemDto> items = inventoryClientService.getMyInventory().stream()
                .filter(item -> ItemStatus.AVAILABLE.name().equals(item.getAvailabilityStatus()))
                .toList();
            inventoryItemComboBox.getItems().setAll(items);

            if (!items.isEmpty()) {
                inventoryItemComboBox.setValue(items.get(0));
            }
            else {
                // dùng để cập nhật selected sản phẩm preview
                updateSelectedItemPreview(null);
            }
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot load inventory.");
            logger.error("Exception occurred", e);
            // dùng để cập nhật selected sản phẩm preview
            updateSelectedItemPreview(null);
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
            // dùng để cập nhật selected sản phẩm preview
            updateSelectedItemPreview(null);
        }
    }

    // dùng để cập nhật selected sản phẩm preview
    private void updateSelectedItemPreview(ItemDto item) {
        if (item == null) {
            selectedItemNameLabel.setText("No item selected");
            selectedDescriptionLabel.setText("Create an inventory item first, then come back to list it.");
            selectedCategoryLabel.setText("-");
            selectedProductTypeLabel.setText("-");
            selectedItemHintLabel.setText("Only available inventory items can be listed.");
            selectedItemImageView.setImage(null);
            return;
        }

        selectedItemNameLabel.setText(defaultText(item.getName(), "Unnamed item"));
        selectedDescriptionLabel.setText(defaultText(item.getDescription(), "No description."));
        selectedCategoryLabel.setText(defaultText(item.getCategory(), "-"));
        selectedProductTypeLabel.setText(defaultText(item.getProductType(), "-"));
        selectedItemHintLabel.setText("Selected item is ready to be locked into this auction.");
        selectedItemImageView.setImage(decodeBase64Image(item.getThumbnailBase64()));
    }

    // dùng để kiểm tra tính hợp lệ inputs
    private void validateInputs() {
        if (inventoryItemComboBox.getValue() == null)
            throw new ValidationException("Please select an inventory item");
        if (startDatePicker.getValue() == null)
            throw new ValidationException("Please select a start date");
        if (endDatePicker.getValue() == null)
            throw new ValidationException("Please select an end date");
    }

    // dùng để phân tích cú pháp số tiền
    private double parseAmount(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);

        try {
            double amount = Double.parseDouble(parseValue);
            if (amount < 0) throw new ValidationException(fieldName + " cannot be negative");
            return amount;
        }
        catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number");
        }
    }

    // dùng để phân tích cú pháp thời gian
    private LocalTime parseTime(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);

        try {
            return LocalTime.parse(parseValue, TIME_FORMATTER);
        }
        catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must use HH:mm format (e.g., 09:30)");
        }
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

    // dùng để default text
    private String defaultText(String value, String fallback) {
        return safe(value).isBlank() ? fallback : value;
    }

    // dùng để safe
    private String safe(String value) {
        return value == null ? "" : value;
    }

    // dùng để liên kết dữ liệu top bar
    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.CREATE_AUCTION, false, null);
    }
}

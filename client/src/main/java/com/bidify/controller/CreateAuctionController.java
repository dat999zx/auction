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
import com.bidify.common.utility.TimeUtil;
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

    @FXML
    private TextField triggerTimeField;

    @FXML
    private TextField extensionTimeField;

    @FXML
    private DatePicker maxEndDatePicker;

    @FXML
    private TextField maxEndTimeField;

    @FXML
    private Label AntiSnipingStatusField;

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
            
            extensionTimeField.setText("00:05");
            triggerTimeField.setText("00:05");
            
            maxEndDatePicker.setEditable(false);
            
            // Set initial defaults for maximum closing ceiling
            syncMaxEndTimeWithStandardEnd();

            // 1. Listen for standard end date modifications
            endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    syncMaxEndTimeWithStandardEnd();
                }
            });

            // 2. Listen for when standard end time loses focus
            endTimeField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                // False means it just lost focus (user finished typing/clicking away)
                if (!isNowFocused) {
                    syncMaxEndTimeWithStandardEnd();
                }
            });

            maxEndDatePicker.valueProperty().addListener((obs, oldVal, newVal)  -> {
                // False means it just lost focus (user finished typing/clicking away)
                if (newVal != null) {
                    validateEndTimeWithMaxEndTime();
                }
            });
            maxEndTimeField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                // False means it just lost focus (user finished typing/clicking away)
                if (!isNowFocused) {
                    validateEndTimeWithMaxEndTime();
                }
            });
        });
    }
    private void validateEndTimeWithMaxEndTime() {
        try {
            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = parseTime(endTimeField.getText(), "End time");

            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

            LocalDate maxEndDate = maxEndDatePicker.getValue();
            LocalTime maxEndTime = parseTime(maxEndTimeField.getText(), "Maximum end time");

            LocalDateTime maxEndDateTime = LocalDateTime.of(maxEndDate, maxEndTime);

            if (maxEndDateTime.isBefore(endDateTime)) {
                NotificationUtil.error("Maximum end date & time cannot be earlier than the standard end time.");
                syncMaxEndTimeWithStandardEnd();

                AntiSnipingStatusField.setText("Anti-Sniping Status: Disabled");
                AntiSnipingStatusField.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

            } else if (maxEndDateTime.isAfter(endDateTime)) {
                // Strictly greater than -> Turn Green
                AntiSnipingStatusField.setText("Anti-Sniping Status: Enabled");
                AntiSnipingStatusField.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

            } else {
                // Exactly Equal -> Explicitly force it to Red/Disabled to override old green states
                AntiSnipingStatusField.setText("Anti-Sniping Status: Disabled");
                AntiSnipingStatusField.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        } catch (ValidationException | DateTimeParseException e) {
            // Silently ignore or show error if it's a final action, 
            // but for a listener, it's safer to just reset the status UI.
            AntiSnipingStatusField.setText("Anti-Sniping Status: Disabled");
            AntiSnipingStatusField.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
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

            if (startDateTime.isBefore(TimeUtil.nowInVietnam().minusMinutes(1))) {
                startDateTime = TimeUtil.nowInVietnam();
                startDatePicker.setValue(startDateTime.toLocalDate());
                startTimeField.setText(startDateTime.toLocalTime().format(TIME_FORMATTER));
            }
            
            String triggerTime = triggerTimeField.getText();
            String extensionTime = extensionTimeField.getText();

            parseDuration(triggerTime, "Trigger window");
            parseDuration(extensionTime, "Extension");
            
            // Parse and handle the rewired maximum closing ceiling properties
            LocalDate maxEndDate = maxEndDatePicker.getValue();
            LocalTime maxEndTime = parseTime(maxEndTimeField.getText(), "Maximum end time");
            LocalDateTime maxEndDateTime = LocalDateTime.of(maxEndDate, maxEndTime);

            // Business logic check: Max absolute limit can't be earlier than your normal scheduled closing time
            if (maxEndDateTime.isBefore(endDateTime)) {
                throw new ValidationException("Maximum end date & time cannot be earlier than the standard end time.");
            }

            /* 
             * NOTE ON PAYLOAD TRANSFER:
             * If your CreateAuctionRequest POJO strictly requires a relative string format (like "01:00") 
             * instead of an absolute timestamp string, replace `maxEndDateTime.toString()` down below with:
             * 
             * java.time.Duration.between(endDateTime, maxEndDateTime)... or standard formatting utilities.
             */
            CreateAuctionRequest data = new CreateAuctionRequest(
                com.bidify.network.SocketClient.getClient().getCurrentUsername(),
                selectedItem.getId(),
                startingPrice,
                minIncrement,
                startDateTime.toString(),
                endDateTime.toString(),
                triggerTime,
                extensionTime,
                maxEndDateTime.toString() 
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
        catch (AuctionException | ValidationException | NumberFormatException | DateTimeParseException e) {
            NotificationUtil.error(e.getMessage());
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        }
        catch (Exception e) {
            NotificationUtil.error("An unexpected error occurred: " + e.getMessage());
            logger.error("Unexpected error in createAuction", e);
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
        if (maxEndDatePicker.getValue() == null)
            throw new ValidationException("Please select a maximum hard cap end date");
    }

    // Tự động đồng bộ hóa thời gian kết thúc tối đa sau khi cập nhật thời gian kết thúc chuẩn
    private void syncMaxEndTimeWithStandardEnd() {
        AntiSnipingStatusField.setText("Anti-Sniping Status: Disabled");
        AntiSnipingStatusField.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        try {
            LocalDate endDate = endDatePicker.getValue();
            if (endDate == null) return;

            // Try to parse whatever is currently inside the text field
            LocalTime endTime = parseTime(endTimeField.getText(), "End time");
            LocalDateTime standardEndDateTime = LocalDateTime.of(endDate, endTime);

            // Add a default extension window ceiling
            LocalDateTime recommendedMaxCeiling = standardEndDateTime;

            // Automatically push values downstream to UI elements
            maxEndDatePicker.setValue(recommendedMaxCeiling.toLocalDate());
            maxEndTimeField.setText(recommendedMaxCeiling.toLocalTime().format(TIME_FORMATTER));
            
        } catch (ValidationException | DateTimeParseException e) {
            // Silently handle incomplete/invalid formats during typing or focus loss
            logger.debug("Could not auto-sync maxEndTime due to incomplete or invalid standard endTime format: {}", e.getMessage());
        }
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

    // dùng để phân tích cú pháp thời gian duration
    private java.time.Duration parseDuration(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);

        if (!parseValue.matches("^\\d+:[0-5]\\d$")) {
            throw new ValidationException(fieldName + " must use H...H:mm format (e.g., 01:30 or 25:00)");
        }

        return TimeUtil.parseHHMM(parseValue);
    }

    // dùng để decode base64image
    private Image decodeBase64Image(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            Image img = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
            if (img.isError()) {
                return null;
            }
            return img;
        }
        catch (Exception e) {
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
package com.bidify.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
import com.bidify.service.AuctionClientService;
import com.bidify.service.InventoryClientService;
import com.bidify.utility.AuctionFormParser;
import com.bidify.utility.ImageCache;
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
            bindTopBar();
            configureInventorySelection();
            loadInventoryItems();

            startDatePicker.setEditable(false);
            startDatePicker.setValue(LocalDate.now());
            endDatePicker.setEditable(false);
            endDatePicker.setValue(LocalDate.now().plusDays(7));
            startTimeField.setText("09:00");
            endTimeField.setText("18:00");
            
            extensionTimeField.setText("00:05");
            triggerTimeField.setText("00:05");
            minIncrementField.setText("1");
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
            LocalTime endTime = AuctionFormParser.parseTime(endTimeField.getText(), "End time");

            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

            LocalDate maxEndDate = maxEndDatePicker.getValue();
            LocalTime maxEndTime = AuctionFormParser.parseTime(maxEndTimeField.getText(), "Maximum end time");

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
    @FXML
    private void createAuction() {
        try {
            validateInputs();

            ItemDto selectedItem = inventoryItemComboBox.getValue();
            double startingPrice = AuctionFormParser.parseAmount(startingPriceField.getText(), "Starting price");
            double minIncrement = AuctionFormParser.parseAmount(minIncrementField.getText(), "Min increment");

            LocalDate startDate = startDatePicker.getValue();
            LocalTime startTime = AuctionFormParser.parseTime(startTimeField.getText(), "Start time");
            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);

            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = AuctionFormParser.parseTime(endTimeField.getText(), "End time");
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

            if (startDateTime.isBefore(TimeUtil.nowInVietnam().minusMinutes(1))) {
                startDateTime = TimeUtil.nowInVietnam();
                startDatePicker.setValue(startDateTime.toLocalDate());
                startTimeField.setText(startDateTime.toLocalTime().format(TIME_FORMATTER));
            }
            
            String triggerTime = triggerTimeField.getText();
            String extensionTime = extensionTimeField.getText();

            AuctionFormParser.parseDuration(triggerTime, "Trigger window");
            AuctionFormParser.parseDuration(extensionTime, "Extension");
            
            // Parse and handle the rewired maximum closing ceiling properties
            LocalDate maxEndDate = maxEndDatePicker.getValue();
            LocalTime maxEndTime = AuctionFormParser.parseTime(maxEndTimeField.getText(), "Maximum end time");
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

    private void configureInventorySelection() {
        inventoryItemComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ItemDto item) {
                if (item == null) return "";
                return item.getName() + " • " + safe(item.getCategory()) + " • " + safe(item.getProductType());
            }

            @Override
            public ItemDto fromString(String string) {
                return null;
            }
        });

        inventoryItemComboBox.valueProperty().addListener((obs, oldValue, newValue) -> updateSelectedItemPreview(newValue));
    }

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
                updateSelectedItemPreview(null);
            }
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot load inventory.");
            logger.error("Exception occurred", e);
            updateSelectedItemPreview(null);
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
            updateSelectedItemPreview(null);
        }
    }

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
        selectedItemImageView.setImage(ImageCache.decode(item.getThumbnailBase64()));
    }

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
            LocalTime endTime = AuctionFormParser.parseTime(endTimeField.getText(), "End time");
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

    private String defaultText(String value, String fallback) {
        return safe(value).isBlank() ? fallback : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.CREATE_AUCTION, false, null);
    }
}
package com.bidify.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdateAuctionRequest;
import com.bidify.common.utility.TimeUtil;
import com.bidify.service.AuctionClientService;
import com.bidify.utility.AuctionFormParser;
import com.bidify.utility.ImageCache;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

public class ModifyAuctionController {
    private static final Logger logger = LoggerFactory.getLogger(ModifyAuctionController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    // Strict ISO format enforcing standard seconds placeholder format rule
    private static final DateTimeFormatter ISO_SERVER_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static String currentAuctionId;

    @FXML private Label itemNameLabel;
    @FXML private Label itemDescriptionLabel;
    @FXML private Label categoryLabel;
    @FXML private Label productTypeLabel;
    @FXML private Label itemStatusLabel;
    @FXML private ImageView linkedItemImageView;
    @FXML private TextField startingPriceField;
    @FXML private TextField minIncrementField;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startTimeField;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endTimeField;
    @FXML private TextField triggerTimeField;
    @FXML private TextField extensionTimeField;
    
    @FXML private DatePicker maxEndDatePicker;
    @FXML private TextField maxEndTimeField;
    @FXML private Label AntiSnipingStatusField;
    
    @FXML private Button cancelButton;
    @FXML private Button saveChangesButton;
    @FXML private Button deleteAuctionButton;

    private final AuctionClientService auctionClientService = new AuctionClientService();

    public static void setAuctionId(String auctionId) {
        currentAuctionId = auctionId;
    }

    @FXML
    private void initialize() {
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            NotificationUtil.error("No auction selected for modification.");
            return;
        }
        
        setupValidationListeners();
        loadAuctionData(currentAuctionId);
    }

    private void setupValidationListeners() {
        Runnable validateDateTimeRule = () -> {
            try {
                LocalDate endDate = endDatePicker.getValue();
                LocalDate maxEndDate = maxEndDatePicker.getValue();
                if (endDate == null || maxEndDate == null) return;

                String endText = endTimeField.getText();
                String maxEndText = maxEndTimeField.getText();
                if (endText == null || endText.trim().isEmpty() || maxEndText == null || maxEndText.trim().isEmpty()) return;

                LocalTime endTime = AuctionFormParser.parseTime(endText.trim(), "End time");
                LocalTime maxEndTime = AuctionFormParser.parseTime(maxEndText.trim(), "Maximum end time");

                LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);
                LocalDateTime maxEndDateTime = LocalDateTime.of(maxEndDate, maxEndTime);

                if (maxEndDateTime.isBefore(endDateTime)) {
                    Platform.runLater(() -> {
                        NotificationUtil.error("Maximum end date & time cannot be earlier than the standard end time.");
                        syncMaxEndTimeWithStandardEnd();
                        AntiSnipingStatusField.setText("Anti-Sniping Status: Disabled");
                        AntiSnipingStatusField.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    });
                } else if (maxEndDateTime.isAfter(endDateTime)) {
                    Platform.runLater(() -> {
                        AntiSnipingStatusField.setText("Anti-Sniping Status: Enabled");
                        AntiSnipingStatusField.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                    });
                } else {
                    Platform.runLater(() -> {
                        AntiSnipingStatusField.setText("Anti-Sniping Status: Disabled");
                        AntiSnipingStatusField.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    });
                }
            } catch (Exception ignored) {
            }
        };

        maxEndTimeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.trim().length() == 5) {
                validateDateTimeRule.run();
            }
        });
        
        endTimeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.trim().length() == 5) {
                validateDateTimeRule.run();
            }
        });

        maxEndDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null) validateDateTimeRule.run(); });
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null) validateDateTimeRule.run(); });
    }

    private void syncMaxEndTimeWithStandardEnd() {
        if (endDatePicker.getValue() != null) {
            maxEndDatePicker.setValue(endDatePicker.getValue());
        }
        if (endTimeField.getText() != null && !endTimeField.getText().isBlank()) {
            maxEndTimeField.setText(endTimeField.getText().trim());
        }
    }

    private void loadAuctionData(String auctionId) {
        Thread loader = new Thread(() -> {
            try {
                AuctionDto auction = auctionClientService.getAuctionDetail(auctionId);
                Platform.runLater(() -> bindAuctionToFields(auction));
            } catch (IOException | AuctionException e) {
                logger.error("Failed to load auction data", e);
                Platform.runLater(() -> NotificationUtil.error("Failed to load auction: " + e.getMessage()));
            }
        });
        loader.setDaemon(true);
        loader.start();
    }

    private void bindAuctionToFields(AuctionDto data) {
        itemNameLabel.setText(defaultText(data.getAuctionName(), "Unnamed item"));
        itemDescriptionLabel.setText(defaultText(data.getDescription(), "No description."));
        categoryLabel.setText(defaultText(data.getCategory(), "-"));
        productTypeLabel.setText(defaultText(data.getProductType(), "-"));
        itemStatusLabel.setText("Linked inventory item is locked by this auction.");
        linkedItemImageView.setImage(ImageCache.decode(data.getThumbnailBase64()));

        startingPriceField.setText(String.valueOf(data.getStartingPrice()));
        minIncrementField.setText(String.valueOf(data.getMinIncrement()));

        triggerTimeField.setText(data.getAntiSnipingTriggerTime() != null ? data.getAntiSnipingTriggerTime() : "00:05");
        extensionTimeField.setText(data.getAntiSnipingExtensionTime() != null ? data.getAntiSnipingExtensionTime() : "00:05");

        try {
            LocalDateTime start = TimeUtil.parseDateTime(data.getStartTime());
            LocalDateTime end = TimeUtil.parseDateTime(data.getEndTime());

            startDatePicker.setValue(start.toLocalDate());
            startTimeField.setText(start.toLocalTime().format(TIME_FORMATTER));
            
            endDatePicker.setValue(end.toLocalDate());
            endTimeField.setText(end.toLocalTime().format(TIME_FORMATTER));

            if (data.getMaxEndTime() != null) {
                LocalDateTime maxEnd = LocalDateTime.parse(data.getMaxEndTime());
                maxEndDatePicker.setValue(maxEnd.toLocalDate());
                maxEndTimeField.setText(maxEnd.toLocalTime().format(TIME_FORMATTER));
                
                if (maxEnd.isAfter(end)) {
                    AntiSnipingStatusField.setText("Anti-Sniping Status: Enabled");
                    AntiSnipingStatusField.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
                } else {
                    AntiSnipingStatusField.setText("Anti-Sniping Status: Disabled");
                    AntiSnipingStatusField.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                }
            } else {
                maxEndDatePicker.setValue(end.toLocalDate());
                maxEndTimeField.setText(end.toLocalTime().format(TIME_FORMATTER));
                AntiSnipingStatusField.setText("Anti-Sniping Status: Disabled");
                AntiSnipingStatusField.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            logger.warn("Could not parse auction dates: {}", e.getMessage());
        }
    }

    @FXML
    private void handleSaveChanges() {
        saveChangesButton.setText("Saving...");
        saveChangesButton.setDisable(true);
        if (currentAuctionId == null) {
            NotificationUtil.error("No auction selected for modification.");
            return;
        }

        try {
            double price = AuctionFormParser.parseAmount(startingPriceField.getText(), "Starting Price");
            
            LocalDate startDate = startDatePicker.getValue();
            LocalTime startTime = AuctionFormParser.parseTime(startTimeField.getText(), "Start Time");
            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = AuctionFormParser.parseTime(endTimeField.getText(), "End Time");

            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

            if (endDateTime.isBefore(startDateTime)) {
                throw new ValidationException("End time cannot be before start time.");
            }

            LocalDate maxEndDate = maxEndDatePicker.getValue();
            LocalTime maxEndTime = AuctionFormParser.parseTime(maxEndTimeField.getText(), "Maximum End Time");
            LocalDateTime maxEndDateTime = LocalDateTime.of(maxEndDate, maxEndTime);

            if (maxEndDateTime.isBefore(endDateTime)) {
                throw new ValidationException("Maximum end date & time cannot be earlier than the standard end time.");
            }

            double minIncrement = AuctionFormParser.parseAmount(minIncrementField.getText(), "Minimum Increment");
            
            String triggerTime = triggerTimeField.getText();
            String extensionTime = extensionTimeField.getText();

            AuctionFormParser.parseDuration(triggerTime, "Trigger Window");
            AuctionFormParser.parseDuration(extensionTime, "Extension");

            // Mapping raw structural payload conversions directly to clear formats
            String finalStartTimeStr = startDateTime.format(ISO_SERVER_FORMATTER);
            String finalEndTimeStr = endDateTime.format(ISO_SERVER_FORMATTER);
            String finalMaxEndTimeStr = maxEndDateTime.format(ISO_SERVER_FORMATTER);

            UpdateAuctionRequest request = new UpdateAuctionRequest(
                currentAuctionId,
                "",
                "",
                price,
                minIncrement, 
                finalStartTimeStr,     // Format: "yyyy-MM-ddTHH:mm:ss"
                finalEndTimeStr,       // Format: "yyyy-MM-ddTHH:mm:ss"
                "Auction details updated by seller.",
                triggerTime,
                extensionTime,
                finalMaxEndTimeStr     // Passed as full absolute timestamp matching exact validation route
            );

            Response response = auctionClientService.updateAuction(request);

            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Auction updated successfully.");
                AuctionDetailsController.setAuctionId(currentAuctionId);
                SceneManager.clearCache("auctiondetail.fxml");
                SceneManager.switchScene("auctiondetail.fxml", false, false);
            } else {
                NotificationUtil.error(response.getMessage());
            }

        } catch (ValidationException | NumberFormatException e) {
            NotificationUtil.error(e.getMessage());
        } catch (IOException e) {
            NotificationUtil.error("Failed to connect to server.");
            logger.error("IOException while updating auction", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        } finally {
            saveChangesButton.setText("Save Changes");
            saveChangesButton.setDisable(false);
        }
    }

    @FXML
    private void handleCancel() {
        SceneManager.switchScene("hub.fxml", false, true);
    }

    @FXML
    private void handleDeleteAuction() {
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            NotificationUtil.error("No auction selected for deletion.");
            return;
        }

        try {
            Response response = auctionClientService.deleteAuction(currentAuctionId);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Auction deleted successfully.");
                SceneManager.switchScene("hub.fxml", false, true);
                return;
            }
            NotificationUtil.error(response.getMessage());
        } catch (IOException e) {
            NotificationUtil.error("Failed to connect to server.");
            logger.error("IOException while deleting auction", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    private String defaultText(String value, String fallback) {
        return safe(value).isBlank() ? fallback : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
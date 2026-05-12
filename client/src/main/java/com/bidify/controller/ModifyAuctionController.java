package com.bidify.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdateAuctionRequest;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.service.AuctionClientService;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ModifyAuctionController {
    private static final Logger logger = LoggerFactory.getLogger(ModifyAuctionController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // This would ideally be set by the previous scene (e.g., the Auction Dashboard)
    // before switching to this view.
    private static String currentAuctionId;

    @FXML private TextField productNameField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> productTypeComboBox;
    @FXML private TextField startingPriceField;
    @FXML private TextField minIncrementField; // Optional: Only if you want to allow editing this field
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startTimeField;
    @FXML private DatePicker endDatePicker;
    @FXML private TextField endTimeField;
    
    // Action Buttons
    @FXML private Button cancelButton;
    @FXML private Button saveChangesButton;

    private final AuctionClientService auctionClientService = new AuctionClientService();

    /**
     * Static method to set the Auction ID to be modified.
     * Call this before switching to modify-auction.fxml
     */
    public static void setAuctionId(String auctionId) {
        currentAuctionId = auctionId;
    }

    @FXML
    private void initialize() {
        // Setup dropdowns
        categoryComboBox.getItems().setAll("Electronics", "Fashion", "Art", "Collectibles", "Vehicles", "Other");
        productTypeComboBox.getItems().setAll("New", "Used", "Rare", "Vintage", "Limited");

        // UI logic: If we have an ID, we should ideally fetch existing data here
        if (currentAuctionId != null) {
            loadAuctionData(currentAuctionId);
        }
    }

    private void loadAuctionData(String auctionId) {
        // Implementation: Fetch data from auctionClientService and populate fields
        // e.g., productNameField.setText(fetchedAuction.getName());
    }

    @FXML
    private void handleSaveChanges() {
        if (currentAuctionId == null) {
            NotificationUtil.error("No auction selected for modification.");
            return;
        }

        try {
            // 1. Collect Data from UI
            String name = productNameField.getText().trim();
            String desc = descriptionArea.getText().trim();
            double price = parseAmount(startingPriceField.getText(), "Starting Price");
            
            LocalDate startDate = startDatePicker.getValue();
            LocalTime startTime = parseTime(startTimeField.getText(), "Start Time");
            LocalDate endDate = endDatePicker.getValue();
            LocalTime endTime = parseTime(endTimeField.getText(), "End Time");

            // 2. Validate Dates
            LocalDateTime startDateTime = LocalDateTime.of(startDate, startTime);
            LocalDateTime endDateTime = LocalDateTime.of(endDate, endTime);

            if (endDateTime.isBefore(startDateTime)) {
                throw new ValidationException("End time cannot be before start time.");
            }

            double minIncrement = parseAmount(minIncrementField.getText(), "Minimum Increment");
            // 3. Construct the Request (Mapping to your UpdateAuctionRequest model)
            // Note: Message is added as a blank/default string if not present in your FXML
            UpdateAuctionRequest request = new UpdateAuctionRequest(
                currentAuctionId,
                name,
                desc,
                price,
                minIncrement, startDateTime.toString(),
                endDateTime.toString(),
                "Auction details updated by seller." // Default message
            );

            // 4. Send to Service
            Response response = auctionClientService.updateAuction(request);

            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Auction updated successfully.");
                SceneManager.switchScene("hub.fxml", false, true);
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
        }
    }

    @FXML
    private void handleCancel() {
        SceneManager.switchScene("hub.fxml", false, true);
    }

    private double parseAmount(String value, String fieldName) {
        String parseValue = (value == null) ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);
        try {
            return Double.parseDouble(parseValue);
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number.");
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        String parseValue = (value == null) ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);
        try {
            return LocalTime.parse(parseValue, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must use HH:mm format.");
        }
    }
}
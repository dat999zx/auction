package com.bidify.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdateAuctionRequest;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.service.AuctionClientService;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
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
        // 1. Setup dropdowns
        categoryComboBox.getItems().setAll("Electronics", "Fashion", "Art", "Collectibles", "Vehicles", "Other");
        productTypeComboBox.getItems().setAll("New", "Used", "Rare", "Vintage", "Limited");

        // 2. Load data if we have an ID
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            NotificationUtil.error("No auction selected for modification.");
            return;
        }
        
        loadAuctionData(currentAuctionId);
    }

    private void loadAuctionData(String auctionId) {
        // Start background thread to fetch data (similar to AuctionDetailsController)
        Thread loader = new Thread(() -> {
            try {
                // Fetch from service
                AuctionDto auction = auctionClientService.getAuctionDetail(auctionId);
                
                // Update UI on the JavaFX Thread
                Platform.runLater(() -> bindAuctionToFields(auction));
                
            } catch (IOException | AuctionException e) {
                logger.error("Failed to load auction data", e);
                Platform.runLater(() -> {
                    NotificationUtil.error("Failed to load auction: " + e.getMessage());
                });
            }
        });
        loader.setDaemon(true);
        loader.start();
    }

    private void bindAuctionToFields(AuctionDto data) {
        // Populate Text Fields
        productNameField.setText(data.getAuctionName());
        descriptionArea.setText(data.getDescription());
        startingPriceField.setText(String.valueOf(data.getStartingPrice()));
        
        // Set ComboBox values (ensure these strings match the lists in initialize)
        categoryComboBox.setValue(data.getCategory()); 
        productTypeComboBox.setValue(data.getProductType());

        // Handle Dates and Times
        // Assuming getStartTime() returns an ISO string like "2023-10-27T09:00"
        try {
            LocalDateTime start = LocalDateTime.parse(data.getStartTime());
            LocalDateTime end = LocalDateTime.parse(data.getEndTime());

            startDatePicker.setValue(start.toLocalDate());
            startTimeField.setText(start.toLocalTime().format(TIME_FORMATTER));
            
            endDatePicker.setValue(end.toLocalDate());
            endTimeField.setText(end.toLocalTime().format(TIME_FORMATTER));
        } catch (Exception e) {
            logger.warn("Could not parse auction dates: {}", e.getMessage());
        }
    }

    @FXML
    private void handleSaveChanges() {
        saveChangesButton.setText("Saving..."); // Prevent multiple clicks
        saveChangesButton.setDisable(true);
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
                //redirect to auction details page
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
        }
        finally {
            saveChangesButton.setText("Save Changes");
            saveChangesButton.setDisable(false);
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
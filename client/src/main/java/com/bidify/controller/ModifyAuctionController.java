package com.bidify.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;

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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ModifyAuctionController {
    private static final Logger logger = LoggerFactory.getLogger(ModifyAuctionController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // This would ideally be set by the previous scene (e.g., the Auction Dashboard)
    // before switching to this view.
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
    
    // Action Buttons
    @FXML private Button cancelButton;
    @FXML private Button saveChangesButton;
    @FXML private Button deleteAuctionButton;

    private final AuctionClientService auctionClientService = new AuctionClientService();

    /**
     * Static method to set the Auction ID to be modified.
     * Call this before switching to modify-auction.fxml
     */
    // dùng để thiết lập đấu giá ID
    public static void setAuctionId(String auctionId) {
        currentAuctionId = auctionId;
    }

    // dùng để khởi tạo
    @FXML
    private void initialize() {
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            NotificationUtil.error("No auction selected for modification.");
            return;
        }
        
        // dùng để tải đấu giá data
        loadAuctionData(currentAuctionId);
    }

    // dùng để tải đấu giá data
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

    // dùng để liên kết dữ liệu đấu giá chuyển thành fields
    private void bindAuctionToFields(AuctionDto data) {
        itemNameLabel.setText(defaultText(data.getAuctionName(), "Unnamed item"));
        itemDescriptionLabel.setText(defaultText(data.getDescription(), "No description."));
        categoryLabel.setText(defaultText(data.getCategory(), "-"));
        productTypeLabel.setText(defaultText(data.getProductType(), "-"));
        itemStatusLabel.setText("Linked inventory item is locked by this auction.");
        linkedItemImageView.setImage(decodeBase64Image(data.getThumbnailBase64()));

        startingPriceField.setText(String.valueOf(data.getStartingPrice()));

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

    // dùng để xử lý lưu changes
    @FXML
    private void handleSaveChanges() {
        saveChangesButton.setText("Saving..."); // Prevent multiple clicks
        saveChangesButton.setDisable(true);
        if (currentAuctionId == null) {
            NotificationUtil.error("No auction selected for modification.");
            return;
        }

        try {
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
            UpdateAuctionRequest request = new UpdateAuctionRequest(
                currentAuctionId,
                "",
                "",
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

    // dùng để xử lý hủy bỏ
    @FXML
    private void handleCancel() {
        SceneManager.switchScene("hub.fxml", false, true);
    }

    // dùng để xử lý xóa đấu giá
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
        }
        catch (IOException e) {
            NotificationUtil.error("Failed to connect to server.");
            logger.error("IOException while deleting auction", e);
        }
        catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    // dùng để phân tích cú pháp số tiền
    private double parseAmount(String value, String fieldName) {
        String parseValue = (value == null) ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);
        try {
            return Double.parseDouble(parseValue);
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number.");
        }
    }

    // dùng để phân tích cú pháp thời gian
    private LocalTime parseTime(String value, String fieldName) {
        String parseValue = (value == null) ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);
        try {
            return LocalTime.parse(parseValue, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must use HH:mm format.");
        }
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
}

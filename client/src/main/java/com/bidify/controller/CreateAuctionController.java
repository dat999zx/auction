package com.bidify.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.service.AuctionClientService;
import com.bidify.service.AuthClientService;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateAuctionController {
    private static final Logger logger = LoggerFactory.getLogger(CreateAuctionController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
    private Button auctionsButton;

    @FXML
    private Button createAuctionButton;

    @FXML
    private Button historyButton;

    private MissionBarController missionBarController;

    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final AuthClientService authClientService = new AuthClientService();

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
    private void handleSelection(ActionEvent event) {
        if (!(event.getSource() instanceof Button clickedButton)) return;

        if (clickedButton == auctionsButton) {
            SceneManager.switchScene("hub.fxml", false, true);
        } else if (clickedButton == historyButton) {
            SceneManager.switchScene("history.fxml", false, true);
        }
    }

    @FXML
    private void handleLogout() {
        String currentUsername = com.bidify.network.SocketClient.getClient().getCurrentUsername();

        if (currentUsername == null || currentUsername.isBlank()) {
            SceneManager.clearAllCache();
            SceneManager.switchScene("login.fxml", true, false);
            return;
        }

        try {
            Response response = authClientService.logout();
            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Logged out successfully.");
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml", true, false);
                return;
            }
            logger.error("Logout failed: {}", response.getMessage());
        } catch (IOException e) {
            logger.error("Cannot connect to server while logging out", e);
            NotificationUtil.error("Logout failed: Connection error");
        }
    }

    @FXML
    private void toggleSidebar() {
        if (missionBarController != null) {
            missionBarController.toggleSidebar();
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

            CreateAuctionRequest data = new CreateAuctionRequest(
                com.bidify.network.SocketClient.getClient().getCurrentUsername(),
                productName,
                description,
                category,
                productType,
                startingPrice,
                minIncrement,
                startDateTime.toString(),
                endDateTime.toString()
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
            NotificationUtil.error("Cannot connect to server");
            logger.error("Create auction failed", e);
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
        missionBarController = SceneManager.getMissionBarController();
        if (missionBarController == null) {
            throw new IllegalStateException("Mission bar was not loaded.");
        }

        auctionsButton = missionBarController.getAuctionsButton();
        createAuctionButton = missionBarController.getCreateAuctionButton();
        historyButton = missionBarController.getHistoryButton();
        
        missionBarController.setShowExplore(true);
        missionBarController.setShowSearch(false);
        missionBarController.setUseInlineLogout(true);
        
        missionBarController.setSelectionHandler(this::handleSelection);
        missionBarController.setExploreHandler(event -> toggleSidebar());
        missionBarController.setLogoutHandler(event -> handleLogout());
        missionBarController.setAvatarHandler(event -> {
            SceneManager.switchScene("user-profile.fxml", false, true);
        });
        
        String username = com.bidify.network.SocketClient.getClient().getCurrentUsername();
        missionBarController.setAvatarText(username != null && !username.isEmpty() ? username.substring(0, 1).toUpperCase() : "?");
        missionBarController.setActiveNavigation(createAuctionButton);
    }
}
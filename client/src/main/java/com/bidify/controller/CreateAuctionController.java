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
import com.bidify.network.SocketClient;
import com.bidify.service.AuctionClientService;
import com.bidify.service.AuthClientService;
import com.bidify.utility.SceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class CreateAuctionController {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label messageLabel;

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

    private MissionBarController missionBarController;

    @FXML
    private Button auctionsButton;

    @FXML
    private Button createAuctionButton;

    @FXML
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final AuthClientService authClientService = new AuthClientService();

    @FXML
    private void initialize() {
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
        }

        if (endDatePicker != null) {
            endDatePicker.setEditable(false);
        }

        if (startTimeField != null) {
            startTimeField.setText("09:00");
        }

        if (endTimeField != null) {
            endTimeField.setText("18:00");
        }
    }

    @FXML
    private void toggleSidebar() {
        if (missionBarController != null) {
            missionBarController.toggleSidebar();
        }
    }

    @FXML
    private void handleSelection(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) {
            return;
        }
        if (selectedButton == auctionsButton) {
            SceneManager.switchScene("hub.fxml", false, true);
            return;
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
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml", true, false);
                return;
            }
            System.err.println("Logout failed: " + response.getMessage());
        } catch (IOException e) {
            System.err.println("Cannot connect to server while logging out");
            e.printStackTrace();
        }
    }
    @FXML
    private void createAuction() {
        try {
            showMessage("", false);
            String productName = productNameField.getText() == null ? "" : productNameField.getText().trim();
            String description = descriptionArea.getText() == null ? "" : descriptionArea.getText().trim();
            String category = categoryComboBox.getValue();
            String productType = productTypeComboBox.getValue();
            double startingPrice = parseAmount(startingPriceField.getText(), "Starting price");
            double minIncrement = parseAmount(minIncrementField.getText(), "Min increment");
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            LocalTime startTime = parseTime(startTimeField.getText(), "Start time");
            LocalTime endTime = parseTime(endTimeField.getText(), "End time");
            LocalDateTime startDateTime;
            LocalDateTime endDateTime;

            startDateTime = LocalDateTime.of(startDate, startTime);
            endDateTime = LocalDateTime.of(endDate, endTime);

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
            System.out.println(response.getMessage());
            if (response.getStatus() == RequestStatus.SUCCESS) {
                showMessage("Create new Auction successfully", true);
                SceneManager.clearCache("create-auction.fxml");
                SceneManager.switchScene("hub.fxml", false, true);
            }
        }
        catch (AuctionException e) {
            showMessage(e.getMessage(), false);
        }
        catch (IOException e) {
            showMessage("Cannot connect to server", false);
            e.printStackTrace();
        }
        catch (ValidationException e) {
            showMessage(e.getMessage(), false);
        }
        catch (NumberFormatException e) {
            showMessage(e.getMessage(), false);
        }
    }
    

    private void showMessage(String message, boolean success) {
        if (messageLabel == null) return;
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        if (!message.isBlank()) {
            messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
        }
    }

    // check valid double trước khi parse để tránh lỗi
    private double parseAmount(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);

        try {
            return Double.parseDouble(parseValue);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(fieldName + " must be a number");
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        String parseValue = value == null ? "" : value.trim();
        ValidationUtil.requiresNonBlank(parseValue, fieldName);

        try {
            return LocalTime.parse(parseValue, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException(fieldName + " must use HH:mm format");
        }
    }

    private void bindTopBar() {
        missionBarController = SceneManager.getMissionBarController();
        if (missionBarController == null) {
            throw new IllegalStateException("Mission bar was not loaded.");
        }

        auctionsButton = missionBarController.getAuctionsButton();
        createAuctionButton = missionBarController.getCreateAuctionButton();
        missionBarController.setShowExplore(true);
        missionBarController.setShowSearch(false);
        missionBarController.setUseInlineLogout(true);
        missionBarController.setSelectionHandler(this::handleSelection);
        missionBarController.setExploreHandler(event -> toggleSidebar());
        missionBarController.setLogoutHandler(event -> handleLogout());
        missionBarController.setAvatarHandler(event -> SceneManager.switchScene("user-profile.fxml", false, true));
        missionBarController.setAvatarText(resolveAvatarLetter());
        missionBarController.setActiveNavigation(createAuctionButton);
    }

    private String resolveAvatarLetter() {
        String username = SocketClient.getClient().getCurrentUsername();
        if (username == null || username.isBlank()) {
            return "Nig";
        }
        return username.substring(0, 1).toUpperCase();
    }

}
package com.bidify.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.LogoutRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.network.SocketClient;
import com.bidify.utility.SceneManager;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class CreateAuctionController {
    private static final Duration SIDEBAR_ANIMATION_DURATION = Duration.millis(160);
    private static final double SIDEBAR_EXPANDED_WIDTH = 250.0;

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
    private TextField minIncrementField;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Button auctionsButton;

    @FXML
    private Button createAuctionButton;

    @FXML
    private StackPane sidebarContainer;

    @FXML
    private VBox sidebarContent;

    private boolean sidebarVisible = true;
    private boolean sidebarAnimating = false;

    @FXML
    private void initialize() {
        createAuctionButton.getStyleClass().removeAll("top-link");

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarContainer.widthProperty());
        clip.heightProperty().bind(sidebarContainer.heightProperty());
        sidebarContainer.setClip(clip);

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
    }

    @FXML
    private void toggleSidebar() {
        if (sidebarAnimating) {
            return;
        }

        sidebarAnimating = true;
        double targetWidth = sidebarVisible ? 0.0 : SIDEBAR_EXPANDED_WIDTH;
        double targetTranslateX = sidebarVisible ? -SIDEBAR_EXPANDED_WIDTH : 0.0;

        TranslateTransition slideTransition = new TranslateTransition(SIDEBAR_ANIMATION_DURATION, sidebarContent);
        slideTransition.setToX(targetTranslateX);

        Timeline resizeTimeline = new Timeline(
            new KeyFrame(
                SIDEBAR_ANIMATION_DURATION,
                new KeyValue(sidebarContainer.prefWidthProperty(), targetWidth),
                new KeyValue(sidebarContainer.minWidthProperty(), targetWidth),
                new KeyValue(sidebarContainer.maxWidthProperty(), targetWidth)
            )
        );

        slideTransition.setOnFinished(event -> {
            sidebarVisible = !sidebarVisible;
            sidebarAnimating = false;
        });

        sidebarContent.setMouseTransparent(sidebarVisible);
        slideTransition.play();
        resizeTimeline.play();
    }

    @FXML
    private void handleSelection(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) {
            return;
        }
        if (selectedButton == auctionsButton) {
            SceneManager.switchScene("hub.fxml");
        }
    }

    @FXML
    private void handleLogout() {
        SocketClient client = SocketClient.getClient();
        String currentUsername = client.getCurrentUsername();

        if (currentUsername == null || currentUsername.isBlank()) {
            SceneManager.clearAllCache();
            SceneManager.switchScene("login.fxml");
            return;
        }

        Request request = new Request(RequestType.LOGOUT, new LogoutRequest());
        try {
            Response response = client.send(request);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                client.setCurrentUsername(null);
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml");
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
            LocalDate startTime = startDatePicker.getValue();
            LocalDate endTime = endDatePicker.getValue();
            LocalDateTime startDateTime;
            LocalDateTime endDateTime;

            SocketClient client = SocketClient.getClient();

            ValidationUtil.requiresNonBlank(productName, "Product name");
            ValidationUtil.requiresNonBlank(description, "Description");
            ValidationUtil.requiresNonBlank(category, "Category");
            ValidationUtil.requiresNonBlank(productType, "Product type");
            ValidationUtil.validatePositiveAmount(startingPrice, "Starting price");
            ValidationUtil.validateMaxLength("description", description, 2000);
            if (startTime == null) throw new ValidationException("Start date cannot be empty");
            if (endTime == null) throw new ValidationException("End date cannot be empty");

            if (minIncrement < 0) throw new ValidationException("min increment should be non-negative");
            if (startTime.isBefore(LocalDate.now())) throw new ValidationException("Start time must be after current time!");
            if (endTime.isBefore(startTime)) throw new ValidationException("End time must be after start time!");

            startDateTime = startTime.atStartOfDay();
            endDateTime = endTime.atTime(LocalTime.MAX);

            CreateAuctionRequest data = new CreateAuctionRequest(
                client.getCurrentUsername(),
                productName,
                description,
                category,
                productType,
                startingPrice,
                minIncrement,
                startDateTime.toString(),
                endDateTime.toString()
            );
            Request request = new Request(RequestType.CREATE_AUCTION, data);

            Response response = client.send(request);
            System.out.println(response.getMessage());
            switch (response.getStatus()) {
                case SUCCESS:
                    showMessage("Create new Auction successfully", true);
                    SceneManager.switchScene("hub.fxml");
                    break;    
                default:
                    throw new AuctionException(response.getMessage());
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
}

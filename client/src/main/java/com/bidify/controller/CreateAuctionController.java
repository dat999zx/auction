package com.bidify.controller;

import java.io.IOException;
import java.time.LocalDate;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
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
    private TextField productNameField;

    @FXML 
    private TextArea descriptionField;

    @FXML
    private ComboBox<String> category;

    @FXML
    private ComboBox<String> type;

    @FXML
    private TextField startingPriceField;

    @FXML 
    private DatePicker endDateField;

    @FXML 
    private DatePicker startDateField;

    @FXML 
    private TextField minIncrementField;

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
    private void createAution(){
        try {
            String productName = productNameField.getText();
            String description = descriptionField.getText();
            Double startingPrice = Double.parseDouble(startingPriceField.getText());
            Double minIncrement = Double.parseDouble(minIncrementField.getText());
            LocalDate startTime = startDateField.getValue();
            LocalDate endTime = endDateField.getValue();

            ValidationUtil.validatePositiveAmount(startingPrice, "Starting price");;

            if (startTime.isBefore(LocalDate.now())) throw new ValidationException("Start time must be after current time!");

            if (endTime.isBefore(startTime)) throw new ValidationException("End time must be after start time!");


        }
    }
}

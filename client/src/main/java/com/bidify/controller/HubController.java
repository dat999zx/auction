package com.bidify.controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.AuctionSummary;
import com.bidify.common.model.LogoutRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.network.SocketClient;
import com.bidify.utility.SceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HubController {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML 
    private TextField searchBar; 

    @FXML
    private Button auctionsButton;

    @FXML
    private Button createAuctionButton;

    @FXML
    private FlowPane liveAuctionsContainer;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private void initialize() {
        auctionsButton.getStyleClass().removeAll("top-link");
    //         button.getStyleClass().removeAll("top-link", "top-link-active");
    //         button.getStyleClass().add(button == activeButton ? "top-link-active" : "top-link");
        loadLiveAuctions();
    }

    @FXML
    private void handleSelection(ActionEvent event) {
        if (event.getSource() instanceof Button selectedButton) {
            if (selectedButton == createAuctionButton) {
                handleCreateAuction();
            }
                
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
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLiveAuctions() {
        try {
            Response response = SocketClient.getClient().send(new Request(RequestType.GET_LIVE_AUCTIONS, null));
            if (response.getStatus() != RequestStatus.SUCCESS) {
                showEmptyState("Cannot load live auctions.");
                return;
            }

            AuctionSummary[] auctions = JsonUtil.fromMap(response.getData(), AuctionSummary[].class);
            if (auctions == null || auctions.length == 0) {
                showEmptyState("No live auctions right now.");
                return;
            }

            emptyStateLabel.setVisible(false);
            emptyStateLabel.setManaged(false);
            liveAuctionsContainer.getChildren().clear();
            for (AuctionSummary auction : auctions) {
                liveAuctionsContainer.getChildren().add(createAuctionCard(auction));
            }
        } catch (IOException e) {
            showEmptyState("Cannot connect to server.");
            e.printStackTrace();
        }
    }

    private void showEmptyState(String message) {
        liveAuctionsContainer.getChildren().clear();
        emptyStateLabel.setText(message);
        if (!emptyStateLabel.getStyleClass().contains("empty-state-label")) {
            emptyStateLabel.getStyleClass().add("empty-state-label");
        }
        emptyStateLabel.setManaged(true);
        emptyStateLabel.setVisible(true);
    }

    private VBox createAuctionCard(AuctionSummary auction) {
        VBox card = new VBox();
        card.setPrefWidth(460);
        card.getStyleClass().add("auction-card");

        StackPane imageWrap = new StackPane();
        imageWrap.setPrefHeight(250);
        imageWrap.getStyleClass().add("card-image-wrap");

        HBox timerPill = new HBox(6);
        timerPill.getStyleClass().add("timer-pill");
        timerPill.setAlignment(Pos.CENTER);
        StackPane.setAlignment(timerPill, Pos.TOP_RIGHT);
        StackPane.setMargin(timerPill, new Insets(16, 16, 0, 0));

        Label timerIcon = new Label("T");
        timerIcon.getStyleClass().add("timer-icon");
        Label timerText = new Label(formatRemainingTime(auction.getEndTime()));
        timerText.getStyleClass().add("timer-text");
        timerPill.getChildren().addAll(timerIcon, timerText);

        Label lotPill = new Label(defaultText(auction.getId(), "Auction"));
        lotPill.getStyleClass().add("lot-pill");
        StackPane.setAlignment(lotPill, Pos.BOTTOM_LEFT);
        StackPane.setMargin(lotPill, new Insets(0, 0, 16, 16));

        imageWrap.getChildren().addAll(timerPill, lotPill);

        VBox body = new VBox(12);
        body.setPadding(new Insets(24));
        body.getStyleClass().add("card-body");

        Label title = new Label(defaultText(auction.getAuctionName(), "Untitled auction"));
        title.setWrapText(true);
        title.getStyleClass().add("card-title");

        Label subtitle = new Label(defaultText(auction.getDescription(), "No description."));
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("card-subtitle");

        HBox bidPanel = new HBox();
        bidPanel.getStyleClass().add("bid-panel");
        bidPanel.setPadding(new Insets(16));

        VBox currentBidBox = new VBox(4);
        Label currentBidLabel = new Label("CURRENT BID");
        currentBidLabel.getStyleClass().add("meta-label");
        Label currentBidValue = new Label(CURRENCY_FORMAT.format(auction.getCurrentBid()));
        currentBidValue.getStyleClass().add("price-text");
        currentBidBox.getChildren().addAll(currentBidLabel, currentBidValue);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox bidCountBox = new VBox(4);
        bidCountBox.setAlignment(Pos.CENTER_RIGHT);
        Label bidCountLabel = new Label("BIDDERS");
        bidCountLabel.getStyleClass().add("meta-label");
        Label bidCountValue = new Label(Integer.toString(auction.getBidCount()));
        bidCountValue.getStyleClass().add("meta-value");
        bidCountBox.getChildren().addAll(bidCountLabel, bidCountValue);

        bidPanel.getChildren().addAll(currentBidBox, spacer, bidCountBox);

        Label sellerLabel = new Label("Seller: " + defaultText(auction.getSeller(), "Unknown"));
        sellerLabel.getStyleClass().add("card-subtitle");

        Button bidButton = new Button("Place Instant Bid");
        bidButton.getStyleClass().add("secondary-action-button");
        bidButton.setOnAction(event -> AuctionDetailsController.openAuctionDetails(auction.getId()));

        body.getChildren().addAll(title, subtitle, sellerLabel, bidPanel, bidButton);
        card.getChildren().addAll(imageWrap, body);
        return card;
    }

    private String formatRemainingTime(String endTime) {
        if (endTime == null || endTime.isBlank()) {
            return "Unknown";
        }
        try {
            Duration duration = Duration.between(LocalDateTime.now(), LocalDateTime.parse(endTime));
            if (duration.isNegative() || duration.isZero()) {
                return "Ended";
            }
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } catch (DateTimeParseException e) {
            return endTime;
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    // private void setActiveTopNav(Button activeButton) {
    //     Button[] topNavButtons = { auctionsButton, createAuctionButton };

    //     for (Button button : topNavButtons) {
    //         if (button == null) {
    //             continue;
    //         }
    //         button.getStyleClass().removeAll("top-link", "top-link-active");
    //         button.getStyleClass().add(button == activeButton ? "top-link-active" : "top-link");
    //     }
    // }

    private void search(){
        if (searchBar.getText() == null || searchBar.getText().isBlank()) return;
        //TODO: search key AuctionName, AuctionId, Auction Category

    }

    private void handleCreateAuction() {
        SceneManager.switchScene("create-auction.fxml");
    }

}

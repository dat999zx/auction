package com.bidify.controller;

import java.io.IOException;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.Response;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.service.AuctionClientService;
import com.bidify.service.AuthClientService;
import com.bidify.utility.SceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AuctionDetailsController {
    private static final String DEFAULT_PREVIEW_IMAGE = "/images/bidify-logo.png";

    private static String selectedAuctionId;

    private MissionBarController missionBarController;
    @FXML
    private Button auctionsButton;
    @FXML
    private Button createAuctionButton;
    @FXML
    private Button logoutButton;
    @FXML
    private Label name;
    @FXML
    private Label description;
    @FXML
    private Label startprice;
    @FXML
    private Label currentprice;
    @FXML
    private Label enddate;
    @FXML
    private TextField inputprice;
    @FXML
    private Button placebid;
    @FXML
    private ImageView previewimage;
    @FXML
    private Label messageLabel;
    @FXML
    private Label latestBidderLabel;
    @FXML
    private Label latestBidAmountLabel;
    @FXML
    private Label latestBidTimeLabel;
    @FXML
    private Label openingBidderLabel;
    @FXML
    private Label openingBidAmountLabel;
    @FXML
    private Label openingBidTimeLabel;

    private double currentDisplayedPrice;
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final AuthClientService authClientService = new AuthClientService();

    public static void openAuctionDetails(String auctionId) {
        selectedAuctionId = auctionId;
        SceneManager.clearCache("auctiondetail.fxml");
        SceneManager.switchScene("auctiondetail.fxml", false, true);
    }

    @FXML
    private void initialize() {
        bindTopBar();
        missionBarController.setActiveNavigation(auctionsButton);
        setPreviewImage(DEFAULT_PREVIEW_IMAGE);
        resetView();
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) {
            showMessage("No auction selected.", false);
            placebid.setDisable(true);
            return;
        }
        loadAuctionDetails(selectedAuctionId);
    }

    @FXML
    private void handlePlaceBid() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) {
            showMessage("No auction selected.", false);
            placebid.setDisable(true);
            return;
        }

        String rawBid = inputprice.getText() == null ? "" : inputprice.getText().trim().replace(",", "");
        if (rawBid.isBlank()) {
            showMessage("Enter a bid amount first.", false);
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(rawBid);
        } catch (NumberFormatException e) {
            showMessage("Bid amount must be a valid number.", false);
            return;
        }

        if (bidAmount <= currentDisplayedPrice) {
            showMessage("Your bid must be higher than the current price.", false);
            return;
        }

        try {
            Response response = auctionClientService.placeBid(selectedAuctionId, bidAmount);
            if (response.getStatus() != RequestStatus.SUCCESS) {
                showMessage(response.getMessage() == null ? "Failed to place bid." : response.getMessage(), false);
                return;
            }

            inputprice.clear();
            showMessage(response.getMessage() == null ? "Bid placed successfully." : response.getMessage(), true);
            loadAuctionDetails(selectedAuctionId);
        } catch (IOException e) {
            showMessage("Cannot connect to server.", false);
            e.printStackTrace();
        } catch (AuctionException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void tomenu() {
        SceneManager.switchScene("hub.fxml", false, true);
    }

    @FXML
    private void handleSelection(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) {
            return;
        }

        if (selectedButton == auctionsButton) {
            missionBarController.setActiveNavigation(auctionsButton);
            tomenu();
            return;
        }

        if (selectedButton == createAuctionButton) {
            missionBarController.setActiveNavigation(createAuctionButton);
            SceneManager.switchScene("create-auction.fxml", false, true);
            return;
        }

        if (selectedButton == logoutButton) {
            handleLogout();
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
            showMessage(response.getMessage() == null ? "Logout failed." : response.getMessage(), false);
        } catch (IOException e) {
            showMessage("Cannot connect to server.", false);
            e.printStackTrace();
        } catch (com.bidify.common.exception.AuthException e) {
            showMessage(e.getMessage(), false);
        }
    }

    private void loadAuctionDetails(String auctionId) {
        try {
            AuctionDto auction = auctionClientService.getAuctionDetail(auctionId);
            bindAuctionData(auction);
            placebid.setDisable(false);
            showMessage("Auction loaded.", true);
        } catch (IOException e) {
            showMessage("Cannot connect to server.", false);
            placebid.setDisable(true);
            e.printStackTrace();
        } catch (AuctionException e) {
            showMessage(e.getMessage(), false);
            placebid.setDisable(true);
        }
    }

    private void bindAuctionData(AuctionDto data) {
        name.setText(DisplayUtil.defaultText(data.getAuctionName(), "Untitled auction"));
        description.setText(DisplayUtil.defaultText(data.getDescription(), "No description."));

        double startingValue = data.getStartingPrice();
        double currentValue = data.getCurrentBid();
        currentDisplayedPrice = currentValue > 0 ? currentValue : startingValue;

        startprice.setText(DisplayUtil.formatCurrency(startingValue));
        currentprice.setText(DisplayUtil.formatCurrency(currentDisplayedPrice));
        enddate.setText(DisplayUtil.formatDateTime(data.getEndTime(), "Unknown"));
        bindActivity(data, startingValue);
        setPreviewImage(DEFAULT_PREVIEW_IMAGE);
    }

    private void bindActivity(AuctionDto data, double startingValue) {
        if (data.getBidCount() > 0) {
            openingBidderLabel.setText("Opening price");
            openingBidAmountLabel.setText(DisplayUtil.formatCurrency(startingValue));
            openingBidTimeLabel.setText("Auction opened");

            latestBidderLabel.setText("Latest bid");
            latestBidAmountLabel.setText(DisplayUtil.formatCurrency(data.getCurrentBid()));
            latestBidTimeLabel.setText("Bid received");
            return;
        }

        latestBidderLabel.setText("No bids yet");
        latestBidAmountLabel.setText("Waiting for first bid");
        latestBidTimeLabel.setText("Live auction");
        openingBidderLabel.setText("Starting price");
        openingBidAmountLabel.setText(DisplayUtil.formatCurrency(startingValue));
        openingBidTimeLabel.setText("Opening bid");
    }

    private void resetView() {
        name.setText("Loading auction...");
        description.setText("Please wait while the auction details are fetched.");
        startprice.setText(DisplayUtil.formatCurrency(0));
        currentprice.setText(DisplayUtil.formatCurrency(0));
        enddate.setText("Loading...");
        messageLabel.setText("");
        latestBidderLabel.setText("Latest bid");
        latestBidAmountLabel.setText("Live value shown above");
        latestBidTimeLabel.setText("Waiting for server data");
        openingBidderLabel.setText("Starting price");
        openingBidAmountLabel.setText("Opening value shown above");
        openingBidTimeLabel.setText("Opening bid");
        currentDisplayedPrice = 0;
        placebid.setDisable(true);
    }

    private void setPreviewImage(String imagePath) {
        if (previewimage == null) {
            return;
        }

        String resolvedPath = (imagePath == null || imagePath.isBlank()) ? DEFAULT_PREVIEW_IMAGE : imagePath;
        try {
            Image image;
            if (resolvedPath.startsWith("http://") || resolvedPath.startsWith("https://")) {
                image = new Image(resolvedPath, true);
            } else {
                var resource = getClass().getResource(resolvedPath.startsWith("/") ? resolvedPath : "/" + resolvedPath);
                image = resource == null
                    ? new Image(getClass().getResourceAsStream(DEFAULT_PREVIEW_IMAGE))
                    : new Image(resource.toExternalForm(), true);
            }
            previewimage.setImage(image);
        } catch (Exception e) {
            previewimage.setImage(new Image(getClass().getResourceAsStream(DEFAULT_PREVIEW_IMAGE)));
        }
    }

    private void showMessage(String message, boolean success) {
        if (messageLabel == null) {
            return;
        }
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        if (!message.isBlank()) {
            messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
        }
    }

    private void bindTopBar() {
        missionBarController = SceneManager.getMissionBarController();
        if (missionBarController == null) {
            throw new IllegalStateException("Mission bar was not loaded.");
        }

        auctionsButton = missionBarController.getAuctionsButton();
        createAuctionButton = missionBarController.getCreateAuctionButton();
        logoutButton = missionBarController.getLogoutLinkButton();

        missionBarController.setShowExplore(false);
        missionBarController.setShowSearch(false);
        missionBarController.setUseInlineLogout(false);
        missionBarController.setSelectionHandler(this::handleSelection);
        missionBarController.setLogoutHandler(event -> handleLogout());
        missionBarController.setAvatarHandler(event -> SceneManager.switchScene("user-profile.fxml", false, true));
        missionBarController.setAvatarText(resolveAvatarLetter());
        missionBarController.setActiveNavigation(auctionsButton);
    }

    private String resolveAvatarLetter() {
        String username = com.bidify.network.SocketClient.getClient().getCurrentUsername();
        if (username == null || username.isBlank()) {
            return "U";
        }
        return username.substring(0, 1).toUpperCase();
    }
}

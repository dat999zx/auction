package com.bidify.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.Event;
import com.bidify.common.model.Response;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.common.utility.JsonUtil;
import com.bidify.event.EventManager;
import com.bidify.service.AuctionClientService;
import com.bidify.service.AuthClientService;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AuctionDetailsController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionDetailsController.class);
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
    private Label opendate;

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
        Platform.runLater(() -> {
            bindTopBar();
            missionBarController.setActiveNavigation(auctionsButton);
            setPreviewImage(DEFAULT_PREVIEW_IMAGE);
            resetView();
        });

        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_UPDATED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleAuctionEnded);

        if (selectedAuctionId == null || selectedAuctionId.isBlank()) {
            Platform.runLater(() -> {
                showMessage("No auction selected.", false);
                placebid.setDisable(true);
            });
            return;
        }
        loadAuctionDetails(selectedAuctionId);
    }

    private void handleLiveUpdate(Event event) {
        if (selectedAuctionId == null || event.getData() == null) return;

        AuctionDto updatedAuction = JsonUtil.fromMap(event.getData(), AuctionDto.class);
        if (updatedAuction != null && selectedAuctionId.equals(updatedAuction.getId())) {
            Platform.runLater(() -> {
                bindAuctionData(updatedAuction);
                showMessage(event.getMessage(), true);
            });
        }
    }

    private void handleAuctionEnded(Event event) {
        if (selectedAuctionId == null || event.getData() == null) return;

        AuctionDto endedAuction = JsonUtil.fromMap(event.getData(), AuctionDto.class);
        if (endedAuction != null && selectedAuctionId.equals(endedAuction.getId())) {
            Platform.runLater(() -> {
                bindAuctionData(endedAuction);
                placebid.setDisable(true);
                showMessage("Auction has ended.", false);
            });
        }
    }

    public void cleanup() {
        if (selectedAuctionId != null) {
            try {
                auctionClientService.leave(selectedAuctionId);
            } catch (Exception e) {
                logger.warn("Failed to leave auction channel: {}", e.getMessage());
            }
        }
        EventManager.getInstance().unsubscribe(EventType.BID_PLACED, this::handleLiveUpdate);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_UPDATED, this::handleLiveUpdate);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_ENDED, this::handleAuctionEnded);
    }

    //bid placing

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
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            showMessage(e.getMessage(), false);
        }
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
            cleanup();
            SceneManager.switchScene("create-auction.fxml", false, true);
            return;
        }

        if (selectedButton == logoutButton) {
            handleLogout();
        }
    }

    //loader

    private void loadAuctionDetails(String auctionId) {
        Thread loader = new Thread(() -> {
            try {
                AuctionDto auction = auctionClientService.getAuctionDetail(auctionId);
                joinAuctionChannel(auctionId);
                Platform.runLater(() -> {
                    bindAuctionData(auction);
                    placebid.setDisable(false);
                    showMessage("Auction loaded.", true);
                });
            } catch (IOException e) {
                logger.error("Exception occurred", e);
                Platform.runLater(() -> {
                    showMessage("Cannot connect to server.", false);
                    placebid.setDisable(true);
                });
            } catch (AuctionException e) {
                Platform.runLater(() -> {
                    showMessage(e.getMessage(), false);
                    placebid.setDisable(true);
                });
            }
        });
        loader.setDaemon(true);
        loader.start();
    }

    private void joinAuctionChannel(String auctionId) {
        try {
            auctionClientService.join(auctionId);
        } catch (IOException e) {
            Platform.runLater(() -> showMessage("Auction loaded, but live bid updates could not be joined.", false));
        } catch (AuctionException e) {
            // Being already joined should not block the detail screen.
        }
    }

    //binding

    private void bindAuctionData(AuctionDto data) {
        //auction name, seller and description
        name.setText(DisplayUtil.defaultText(data.getAuctionName(), "Untitled auction"));
        openingBidderLabel.setText(DisplayUtil.defaultText(data.getSellerUsername(), "Unknown seller"));
        description.setText(DisplayUtil.defaultText(data.getDescription(), "No description."));

        // bid values and date
        double startingValue = data.getStartingPrice();
        double currentValue = data.getCurrentBid();
        currentDisplayedPrice = currentValue > 0 ? currentValue : startingValue;
        
        openingBidAmountLabel.setText(DisplayUtil.formatCurrency(startingValue));
        currentprice.setText(DisplayUtil.formatCurrency(currentDisplayedPrice));
        opendate.setText(DisplayUtil.formatDateTime(data.getStartTime(), "Unknown"));
        enddate.setText(DisplayUtil.formatDateTime(data.getEndTime(), "Unknown"));

        // validate and display latest bid info     
        if (currentValue == 0) {
            latestBidderLabel.setText("No bids placed yet.");
            latestBidAmountLabel.setText("");
            latestBidTimeLabel.setText("");
        } else {
            latestBidderLabel.setText(data.getCurrentBidderUsername()); 
            latestBidAmountLabel.setText(DisplayUtil.formatCurrency(data.getCurrentBid()));
            latestBidTimeLabel.setText(DisplayUtil.formatDateTime(data.getCreatedAt(), "Unknown"));
        }

        setPreviewImage(DEFAULT_PREVIEW_IMAGE);
    }

    private void resetView() {
        name.setText("Loading auction...");
        description.setText("Please wait while the auction details are fetched.");

        openingBidAmountLabel.setText("Loading...");
        currentprice.setText("Loading...");

        enddate.setText("Loading...");
        messageLabel.setText("");

        latestBidderLabel.setText("Latest bid");
        latestBidAmountLabel.setText("Live value shown above");
        latestBidTimeLabel.setText("Waiting for server data");

        openingBidderLabel.setText("Starting price");
        opendate.setText("Opening bid");

        currentDisplayedPrice = 0;
        placebid.setDisable(true);
    }

    // top bar handlers and miscellaneous

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

    @FXML
    private void handleLogout() {
        String currentUsername = com.bidify.network.SocketClient.getClient().getCurrentUsername();

        if (currentUsername == null || currentUsername.isBlank()) {
            cleanup();
            SceneManager.clearAllCache();
            SceneManager.switchScene("login.fxml", true, false);
            return;
        }

        try {
            Response response = authClientService.logout();
            if (response.getStatus() == RequestStatus.SUCCESS) {
                cleanup();
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml", true, false);
                return;
            }
            showMessage(response.getMessage() == null ? "Logout failed." : response.getMessage(), false);
        } catch (IOException e) {
            showMessage("Cannot connect to server.", false);
            logger.error("Exception occurred", e);
        } catch (com.bidify.common.exception.AuthException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void tomenu() {
        cleanup();
        SceneManager.switchScene("hub.fxml", false, true);
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
        missionBarController.setAvatarHandler(event -> {
            cleanup();
            SceneManager.switchScene("user-profile.fxml", false, true);
        });
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

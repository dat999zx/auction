package com.bidify.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

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
import com.bidify.utility.ImageCache;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

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
    private GridPane thumbnailGrid;
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
        SceneManager.switchScene("auctiondetail.fxml", false, false);
    }

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            // bindTopBar();
            missionBarController.setActiveNavigation(auctionsButton);
            setPreviewImage(DEFAULT_PREVIEW_IMAGE);
            resetView();
        });

        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_UPDATED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleAuctionEnded);
        EventManager.getInstance().subscribe(EventType.AUCTION_DELETED, this::handleAuctionDeleted);

        if (selectedAuctionId == null || selectedAuctionId.isBlank()) {
            Platform.runLater(() -> {
                NotificationUtil.error("No auction selected.");
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
                NotificationUtil.info(event.getMessage());
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
                NotificationUtil.info("Auction has ended.");
            });
        }
    }

    private void handleAuctionDeleted(Event event) {
        if (selectedAuctionId == null || event == null || event.getData() == null) return;

        String deletedAuctionId = String.valueOf(event.getData());
        if (!selectedAuctionId.equals(deletedAuctionId)) return;

        Platform.runLater(() -> {
            placebid.setDisable(true);
            inputprice.clear();
            NotificationUtil.info("This auction was deleted.");

            Thread redirectThread = new Thread(() -> {
                try {
                    Thread.sleep(250);
                    Platform.runLater(this::tomenu);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            redirectThread.setDaemon(true);
            redirectThread.start();
        });
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
        EventManager.getInstance().unsubscribe(EventType.AUCTION_DELETED, this::handleAuctionDeleted);
    }

    //bid placing

    @FXML
    private void handlePlaceBid() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) {
            NotificationUtil.error("No auction selected.");
            placebid.setDisable(true);
            return;
        }

        String rawBid = inputprice.getText() == null ? "" : inputprice.getText().trim().replace(",", "");
        if (rawBid.isBlank()) {
            NotificationUtil.error("Enter a bid amount first.");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(rawBid);
        } catch (NumberFormatException e) {
            NotificationUtil.error("Bid amount must be a valid number.");
            return;
        }

        if (bidAmount <= currentDisplayedPrice) {
            NotificationUtil.error("Your bid must be higher than the current price.");
            return;
        }

        try {
            Response response = auctionClientService.placeBid(selectedAuctionId, bidAmount);
            if (response.getStatus() != RequestStatus.SUCCESS) {
                NotificationUtil.error(response.getMessage() == null ? "Failed to place bid." : response.getMessage());
                return;
            }

            inputprice.clear();
            NotificationUtil.success(response.getMessage() == null ? "Bid placed successfully." : response.getMessage());
            loadAuctionDetails(selectedAuctionId);
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
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
                });
            } catch (IOException e) {
                logger.error("Exception occurred", e);
                Platform.runLater(() -> {
                    NotificationUtil.error("Cannot connect to server.");
                    placebid.setDisable(true);
                });
            } catch (AuctionException e) {
                Platform.runLater(() -> {
                    NotificationUtil.error(e.getMessage());
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
            Platform.runLater(() -> NotificationUtil.error("Auction loaded, but live bid updates could not be joined."));
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

        // set primary image
        if (data.getThumbnailBase64() != null)
            setPreviewImageFromBase64(data.getThumbnailBase64());
        else
            setPreviewImage(DEFAULT_PREVIEW_IMAGE);

        // setup gallery
        setupThumbnailGallery(data);
    }

    private void setupThumbnailGallery(AuctionDto data) {
        if (thumbnailGrid == null) return;
        thumbnailGrid.getChildren().clear();

        if (data.getGalleryBase64() == null || data.getGalleryBase64().isEmpty()) return;

        int col = 0;
        int index = 0;
        for (String base64 : data.getGalleryBase64()) {
            if (col >= 4) break; // limit 4

            StackPane thumbPane = new StackPane();
            thumbPane.getStyleClass().add("thumb-card");
            thumbPane.setPrefHeight(80.0);

            String cacheKey = "auction_" + data.getId() + "_gallery_" + index++;
            Image img = ImageCache.getInstance().get(cacheKey, base64);
            
            if (img != null) {
                ImageView thumbView = new ImageView(img);
                thumbView.setFitHeight(70.0);
                thumbView.setFitWidth(150.0);
                thumbView.setPreserveRatio(true);
                thumbView.setSmooth(true);

                thumbPane.getChildren().add(thumbView);
                thumbPane.setOnMouseClicked(e -> previewimage.setImage(img));
                thumbPane.setStyle("-fx-cursor: hand;");

                thumbnailGrid.add(thumbPane, col++, 0);
            }
        }
    }

    private void resetView() {
        name.setText("Loading auction...");
        description.setText("Please wait while the auction details are fetched.");

        openingBidAmountLabel.setText("Loading...");
        currentprice.setText("Loading...");

        enddate.setText("Loading...");

        latestBidderLabel.setText("Latest bid");
        latestBidAmountLabel.setText("Live value shown above");
        latestBidTimeLabel.setText("Waiting for server data");

        openingBidderLabel.setText("Starting price");
        opendate.setText("Opening bid");

        if (thumbnailGrid != null) thumbnailGrid.getChildren().clear();

        currentDisplayedPrice = 0;
        placebid.setDisable(true);
    }

    // top bar handlers and miscellaneous

    private void setPreviewImageFromBase64(String base64) {
        if (selectedAuctionId != null && base64 != null) {
            String cacheKey = "auction_" + selectedAuctionId + "_thumb";
            Image cachedImage = ImageCache.getInstance().get(cacheKey, base64);
            if (cachedImage != null) {
                previewimage.setImage(cachedImage);
                return;
            }
        }
        setPreviewImage(DEFAULT_PREVIEW_IMAGE);
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
                NotificationUtil.success("Logged out successfully.");
                cleanup();
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml", true, false);
                return;
            }
            NotificationUtil.error(response.getMessage() == null ? "Logout failed." : response.getMessage());
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (com.bidify.common.exception.AuthException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void tomenu() {
        cleanup();
        SceneManager.switchScene("hub.fxml", false, true);
    }

}

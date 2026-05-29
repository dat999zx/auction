package com.bidify.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionResolutionAction;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.AuthException;
import com.bidify.common.model.Event;
import com.bidify.common.model.Response;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.common.utility.JsonUtil;
import com.bidify.event.EventManager;
import com.bidify.model.ClientSession;
import com.bidify.common.dto.PublicProfileDto;
import com.bidify.service.AuctionClientService;
import com.bidify.service.AuthClientService;
import com.bidify.service.PublicProfileClientService;
import com.bidify.utility.AuctionActivityRenderer;
import com.bidify.utility.AuctionBiddingChartRenderer;
import com.bidify.utility.AuctionImageCarousel;
import com.bidify.utility.AuctionSettlementViewState;
import com.bidify.utility.ImageCache;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;
import com.bidify.utility.SoundUtil;
import com.bidify.utility.UiUpdateScheduler;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
    private Label fullCurrentPriceLabel;
    @FXML
    private Label enddate;
    @FXML
    private TextField inputprice;
    @FXML
    private Button placebid;
    @FXML
    private TextField autoBidMaxInput;
    @FXML
    private Button saveAutoBidButton;
    @FXML
    private Button disableAutoBidButton;
    @FXML
    private Label autoBidStatusLabel;
    @FXML
    private ImageView previewimage;
    @FXML
    private Button prevImageButton;
    @FXML
    private Button nextImageButton;
    @FXML
    private HBox thumbnailContainer;

    private final AuctionImageCarousel carousel = new AuctionImageCarousel();
    private final AuctionActivityRenderer activityRenderer = new AuctionActivityRenderer();
    @FXML
    private Label openingBidderLabel;
    @FXML
    private Label sellerReputationLabel;
    @FXML
    private Label openingBidAmountLabel;
    @FXML
    private Label fullStartingPriceLabel;
    @FXML
    private Label opendate;
    @FXML
    private Label leftMetricLabel;
    @FXML
    private Label rightMetricLabel;
    @FXML
    private Label openDateLabel;
    @FXML
    private Label endDateLabel;
    @FXML
    private VBox AntiSnipingVisualBox;
    @FXML
    private Label AntiSnipingVisualLabel;
    @FXML
    private HBox audienceStatsRow;
    @FXML
    private Label watcherCountLabel;
    @FXML
    private Label activeBidderCountLabel;
    @FXML
    private Label recentActivityLabel;
    @FXML
    private VBox recentActivitySection;
    @FXML
    private VBox activityList;
    @FXML
    private VBox bidActionSection;
    @FXML
    private VBox settlementActionSection;
    @FXML
    private Label settlementStatusLabel;
    @FXML
    private Button payNowButton;
    @FXML
    private Button confirmDeliveryButton;
    @FXML
    private Button adminCompleteButton;
    @FXML
    private Button adminCancelButton;
    @FXML
    private VBox analyticsSection;
    @FXML
    private StackPane biddingChartHost;
    @FXML
    private Label biddingChartStateLabel;
    @FXML
    private Label biddingTrendMetricLabel;
    @FXML
    private Label biddingTrendChangeLabel;
    @FXML
    private Button sellerCancelButton;

    private double currentDisplayedPrice;
    private AuctionDto currentAuction;
    private String timerSubscriptionId;
    private boolean currentUserAutoBidActive;
    private Double currentUserAutoBidMax;
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final AuthClientService authClientService = new AuthClientService();
    private final PublicProfileClientService publicProfileClientService = new PublicProfileClientService();
    private double minIncrement;
    private double currentValue;
    private AuctionBiddingChartRenderer biddingChartRenderer;
    public static void setAuctionId(String auctionId) {
        selectedAuctionId = auctionId;
    }

    @FXML
    private void initialize() {
        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_UPDATED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleAuctionEnded);
        EventManager.getInstance().subscribe(EventType.AUCTION_DELETED, this::handleAuctionDeleted);
        biddingChartRenderer = new AuctionBiddingChartRenderer(
                biddingChartHost,
                biddingChartStateLabel,
                biddingTrendMetricLabel,
                biddingTrendChangeLabel);
        biddingChartRenderer.initialize();

        if (openingBidderLabel != null) {
            openingBidderLabel.setOnMouseClicked(event -> {
                if (currentAuction != null && currentAuction.getSellerUsername() != null && !currentAuction.getSellerUsername().isBlank()) {
                    PublicProfileController.setTargetUsername(currentAuction.getSellerUsername());
                    SceneManager.clearCache("public-profile.fxml");
                    SceneManager.switchScene("public-profile.fxml", false, true);
                }
            });
        }

        carousel.bind(previewimage, prevImageButton, nextImageButton, thumbnailContainer, DEFAULT_PREVIEW_IMAGE);

        if (selectedAuctionId != null && !selectedAuctionId.isBlank()) {
            loadAuctionDetails(selectedAuctionId);
        } else {
            NotificationUtil.error("No auction selected.");
        }
    }

    private void handleLiveUpdate(Event event) {
        if (selectedAuctionId == null || event.getData() == null) return;

        AuctionDto updatedAuction = JsonUtil.fromMap(event.getData(), AuctionDto.class);
        if (updatedAuction != null && selectedAuctionId.equals(updatedAuction.getId())) {
            Platform.runLater(() -> {
                updatedAuction.setCurrentUserAutoBidActive(currentUserAutoBidActive);
                updatedAuction.setCurrentUserAutoBidMax(currentUserAutoBidMax);
                bindAuctionData(updatedAuction);
                if (event.getType() == EventType.BID_PLACED) {
                    SoundUtil.success();
                    NotificationUtil.info(event.getMessage());
                }
            });
        }
    }

    private void handleAuctionEnded(Event event) {
        if (selectedAuctionId == null || event.getData() == null) return;

        AuctionDto endedAuction = JsonUtil.fromMap(event.getData(), AuctionDto.class);
        if (endedAuction != null && selectedAuctionId.equals(endedAuction.getId())) {
            Platform.runLater(() -> {
                SoundUtil.success();
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
        stopTimer();
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
            NotificationUtil.error("Your bid must be higher than " + DisplayUtil.formatCurrency(currentDisplayedPrice + minIncrement));
            return;
        }

        try {
            Response response = auctionClientService.placeBid(selectedAuctionId, bidAmount);
            if (response.getStatus() != RequestStatus.SUCCESS) {
                NotificationUtil.error(response.getMessage() == null ? "Failed to place bid." : response.getMessage());
                return;
            }

            SoundUtil.success();
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
    private void handleSaveAutoBid() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) {
            NotificationUtil.error("No auction selected.");
            return;
        }

        String rawMaxBid = autoBidMaxInput.getText() == null ? "" : autoBidMaxInput.getText().trim().replace(",", "");
        if (rawMaxBid.isBlank()) {
            NotificationUtil.error("Enter an AutoBid max first.");
            return;
        }

        double maxBid;
        try {
            maxBid = Double.parseDouble(rawMaxBid);
        } catch (NumberFormatException e) {
            NotificationUtil.error("AutoBid max must be a valid number.");
            return;
        }

        try {
            Response response = auctionClientService.setAutoBid(selectedAuctionId, maxBid);
            autoBidMaxInput.clear();
            autoBidStatusLabel.setText("AutoBid active");
            NotificationUtil.success(response.getMessage() == null ? "AutoBid saved successfully." : response.getMessage());
            loadAuctionDetails(selectedAuctionId);
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void handleDisableAutoBid() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) {
            NotificationUtil.error("No auction selected.");
            return;
        }

        try {
            Response response = auctionClientService.disableAutoBid(selectedAuctionId);
            autoBidStatusLabel.setText("AutoBid disabled");
            NotificationUtil.success(response.getMessage() == null ? "AutoBid disabled successfully." : response.getMessage());
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

    private void loadAuctionDetails(String auctionId) {
        Thread loader = new Thread(() -> {
            try {
                AuctionDto auction = auctionClientService.getAuctionDetail(auctionId);
                joinAuctionChannel(auctionId);
                Platform.runLater(() -> {
                    currentUserAutoBidActive = auction.isCurrentUserAutoBidActive();
                    bindAuctionData(auction);
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
        }
    }

    private void bindAuctionData(AuctionDto data) {
        AuctionDto previousAuction = currentAuction;
        if (data.getGalleryBase64() == null
                && previousAuction != null
                && data.getId() != null
                && data.getId().equals(previousAuction.getId())) {
            data.setGalleryBase64(previousAuction.getGalleryBase64());
        }

        currentAuction = data;
        currentUserAutoBidActive = data.isCurrentUserAutoBidActive();
        currentUserAutoBidMax = data.getCurrentUserAutoBidMax();
        boolean isUpcoming = "UPCOMING".equalsIgnoreCase(data.getStatus());

        name.setText(DisplayUtil.defaultText(data.getAuctionName(), "Untitled auction"));
        openingBidderLabel.setText(DisplayUtil.defaultText(data.getSellerUsername(), "Unknown seller"));
        description.setText(DisplayUtil.defaultText(data.getDescription(), "No description."));
        loadSellerReputation(data.getSellerUsername());

        double startingValue = data.getStartingPrice();
        this.currentValue= data.getCurrentBid();
        this.minIncrement = data.getMinIncrement();
        currentDisplayedPrice = currentValue > 0 ? currentValue : startingValue;
        
        openingBidAmountLabel.setText(DisplayUtil.formatCashSuffix(startingValue));
        fullStartingPriceLabel.setText(DisplayUtil.formatCurrency(startingValue));
        currentprice.setText(DisplayUtil.formatCashSuffix(currentDisplayedPrice));
        fullCurrentPriceLabel.setText(DisplayUtil.formatCurrency(currentDisplayedPrice));
        opendate.setText(DisplayUtil.formatDateTime(data.getStartTime(), "Unknown"));
        configureAuctionState(data.getStatus(), startingValue, currentValue);
        startTimer();

        renderRecentActivity(data, isUpcoming);
        biddingChartRenderer.render(data, isUpcoming);
        refreshAutoBidStatusLabel();
        renderAudienceStats(data, isUpcoming);

        carousel.reset();

        if (AntiSnipingVisualLabel != null) {
            String currentUser = ClientSession.getInstance().getCurrentUsername();
            boolean isAdmin = ClientSession.getInstance().isAdmin();
            boolean isSeller = currentUser != null && currentUser.equals(data.getSellerUsername());
            boolean hasConfig = data.getAntiSnipingTriggerTime() != null
                    && data.getAntiSnipingExtensionTime() != null
                    && data.getMaxEndTime() != null
                    && data.getEndTime() != null
                    && data.getMaxEndTime().compareTo(data.getEndTime()) > 0;

            boolean shouldShow = (isSeller || isAdmin);
            
            AntiSnipingVisualBox.setVisible(shouldShow);
            AntiSnipingVisualBox.setManaged(shouldShow);
            if (shouldShow) {
                if (hasConfig){
                    AntiSnipingVisualLabel.setText("Anti-sniping: trigger:" + data.getAntiSnipingTriggerTime() + " - extension: " + data.getAntiSnipingExtensionTime() + " - Max end Time: " + DisplayUtil.formatDateTime(data.getMaxEndTime(), "Unknown"));
                } else {
                    AntiSnipingVisualLabel.setText("Anti-sniping not configured for this auction.");
                }
            }
        }

        String thumbnailBase64 = data.getThumbnailBase64();
        if (thumbnailBase64 != null && !thumbnailBase64.isBlank()) {
            String cacheKey = "auction_" + data.getId() + "_thumb";
            Image img = ImageCache.getInstance().get(cacheKey, thumbnailBase64);
            if (img != null && !img.isError()) {
                carousel.addImage(img);
            }
        }

        if (data.getGalleryBase64() != null) {
            int index = 0;
            for (String base64 : data.getGalleryBase64()) {
                if (base64 != null && !base64.isBlank()) {
                    if (base64.equals(thumbnailBase64)) {
                        index++;
                        continue;
                    }
                    String cacheKey = "auction_" + data.getId() + "_gallery_" + index++;
                    Image img = ImageCache.getInstance().get(cacheKey, base64);
                    if (img != null && !img.isError() && !carousel.getImages().contains(img)) {
                        carousel.addImage(img);
                    }
                }
            }
        }

        carousel.updateDisplay();


    }

    private void configureAuctionState(String status, double startingValue, double currentValue) {
        boolean isUpcoming = "UPCOMING".equalsIgnoreCase(status);
        boolean isActive = "ACTIVE".equalsIgnoreCase(status);
        boolean isAdmin = ClientSession.getInstance().isAdmin();
        String currentUsername = ClientSession.getInstance().getCurrentUsername();

        leftMetricLabel.setText("STARTING PRICE");
        rightMetricLabel.setText(isUpcoming ? "OPENING BID" : "CURRENT BID");
        currentprice.setText(DisplayUtil.formatCashSuffix(isUpcoming ? startingValue : (currentValue > 0 ? currentValue : startingValue)));
        openDateLabel.setText(isUpcoming ? "Starts at:" : "Open at:");
        endDateLabel.setText("Ends at:");
        recentActivityLabel.setText(isUpcoming ? "AUCTION STATUS" : "RECENT ACTIVITY");

        boolean isSeller = currentUsername != null && currentAuction != null && currentUsername.equals(currentAuction.getSellerUsername());

        if (isActive && !isAdmin && !isSeller) {
            bidActionSection.setManaged(true);
            bidActionSection.setVisible(true);
            placebid.setDisable(false);
        } else {
            bidActionSection.setManaged(false);
            bidActionSection.setVisible(false);
            placebid.setDisable(true);
            inputprice.clear();
        }

        AuctionSettlementViewState settlementState = AuctionSettlementViewState.resolve(
            status,
            currentAuction == null ? null : currentAuction.getCurrentBidderUsername(),
            currentAuction == null ? null : currentAuction.getSellerUsername(),
            currentUsername,
            isAdmin
        );

        if (sellerCancelButton != null) {
            sellerCancelButton.setManaged(settlementState.showSellerCancelButton());
            sellerCancelButton.setVisible(settlementState.showSellerCancelButton());
        }

        if (settlementActionSection != null) {
            settlementActionSection.setManaged(settlementState.showSettlementSection());
            settlementActionSection.setVisible(settlementState.showSettlementSection());
            payNowButton.setManaged(settlementState.showPayButton());
            payNowButton.setVisible(settlementState.showPayButton());
            confirmDeliveryButton.setManaged(settlementState.showConfirmDeliveryButton());
            confirmDeliveryButton.setVisible(settlementState.showConfirmDeliveryButton());
            adminCompleteButton.setManaged(settlementState.showAdminCompleteButton());
            adminCompleteButton.setVisible(settlementState.showAdminCompleteButton());
            adminCancelButton.setManaged(settlementState.showAdminCancelButton());
            adminCancelButton.setVisible(settlementState.showAdminCancelButton());
            if (settlementState.showSettlementSection()) {
                String statusText = "";
                if ("AWAITING_PAYMENT".equalsIgnoreCase(status)) {
                    statusText = "Status: AWAITING PAYMENT (Winner: " + (currentAuction != null ? currentAuction.getCurrentBidderUsername() : "") + ")";
                } else if ("AWAITING_DELIVERY".equalsIgnoreCase(status)) {
                    statusText = "Status: AWAITING DELIVERY";
                } else if ("COMPLETED".equalsIgnoreCase(status)) {
                    statusText = "Status: COMPLETED (Winner: " + (currentAuction != null ? currentAuction.getCurrentBidderUsername() : "") + " paid and received)";
                }
                settlementStatusLabel.setText(statusText);
            }
        }
    }

    private void renderAudienceStats(AuctionDto data, boolean isUpcoming) {
        if (audienceStatsRow == null)
            return;

        audienceStatsRow.setManaged(!isUpcoming);
        audienceStatsRow.setVisible(!isUpcoming);
        if (isUpcoming)
            return;

        watcherCountLabel.setText(formatCount(data.getWatcherCount(), "watching", "watching"));
        activeBidderCountLabel.setText(formatCount(data.getActiveBidderCount(), "active bidder", "active bidders"));
    }

    private void refreshAutoBidStatusLabel() {
        if (autoBidStatusLabel == null) {
            return;
        }

        autoBidStatusLabel.getStyleClass().removeAll("autobid-status-active", "autobid-status-inactive");
        if (currentUserAutoBidActive) {
            autoBidStatusLabel.setText("AutoBid active");
            autoBidStatusLabel.getStyleClass().add("autobid-status-active");
            if (autoBidMaxInput != null && currentUserAutoBidMax != null) {
                autoBidMaxInput.setText(formatAutoBidValue(currentUserAutoBidMax));
            }
        } else {
            autoBidStatusLabel.setText("No AutoBid configured");
            autoBidStatusLabel.getStyleClass().add("autobid-status-inactive");
            if (autoBidMaxInput != null) {
                autoBidMaxInput.clear();
            }
        }
    }

    private String formatAutoBidValue(double value) {
        if (Math.rint(value) == value) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    private String formatCount(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    private void startTimer() {
        stopTimer();
        if (currentAuction == null) return;

        timerSubscriptionId = UiUpdateScheduler.getInstance().subscribe(this::refreshTimerText);
    }

    private void stopTimer() {
        if (timerSubscriptionId == null || timerSubscriptionId.isBlank())
            return;

        UiUpdateScheduler.getInstance().unsubscribe(timerSubscriptionId);
        timerSubscriptionId = null;
    }

    private void refreshTimerText() {
        if (currentAuction == null) {
            enddate.setText("Unknown");
            return;
        }

        boolean isUpcoming = "UPCOMING".equalsIgnoreCase(currentAuction.getStatus());
        String targetTime = isUpcoming ? currentAuction.getStartTime() : currentAuction.getEndTime();
        enddate.setText(DisplayUtil.formatRemainingTime(targetTime));
    }



    private void renderRecentActivity(AuctionDto data, boolean isUpcoming) {
        activityRenderer.render(activityList, data, isUpcoming);
    }

    @FXML
    private void handleLogout() {
        String currentUsername = ClientSession.getInstance().getCurrentUsername();

        if (currentUsername == null || currentUsername.isBlank()) {
            cleanup();
            SceneManager.clearAllCache();
            SceneManager.resetMissionBar();
            SceneManager.preloadAuthScenes();
            SceneManager.switchScene("login.fxml", true, false);
            return;
        }

        try {
            Response response = authClientService.logout();
            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Logged out successfully.");
                cleanup();
                SceneManager.clearAllCache();
                SceneManager.resetMissionBar();
                SceneManager.preloadAuthScenes();
                SceneManager.switchScene("login.fxml", true, false);
                return;
            }
            NotificationUtil.error(response.getMessage() == null ? "Logout failed." : response.getMessage());
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuthException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void tomenu() {
        cleanup();
        SceneManager.switchScene("hub.fxml", false, true);
    }

    @FXML
    private void handlePrevImage() {
        carousel.navigatePrev();
    }

    @FXML
    private void handleNextImage() {
        carousel.navigateNext();
    }

    @FXML
    private void handlePayNow() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) return;
        try {
            Response response = auctionClientService.payAuction(selectedAuctionId);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                SoundUtil.success();
                NotificationUtil.success("Paid for auction successfully!");
                loadAuctionDetails(selectedAuctionId);
            } else {
                NotificationUtil.error(response.getMessage() == null ? "Payment failed." : response.getMessage());
            }
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void handleConfirmDelivery() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) return;
        try {
            Response response = auctionClientService.confirmAuctionDelivery(selectedAuctionId);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                SoundUtil.success();
                NotificationUtil.success("Delivery confirmed successfully!");
                loadAuctionDetails(selectedAuctionId);
            } else {
                NotificationUtil.error(response.getMessage() == null ? "Delivery confirmation failed." : response.getMessage());
            }
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void handleAdminComplete() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) return;
        try {
            Response response = auctionClientService.resolveAuction(selectedAuctionId, AuctionResolutionAction.COMPLETE);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                SoundUtil.success();
                NotificationUtil.success("Auction resolved as completed successfully!");
                loadAuctionDetails(selectedAuctionId);
            } else {
                NotificationUtil.error(response.getMessage() == null ? "Resolution failed." : response.getMessage());
            }
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void handleAdminCancel() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) return;
        try {
            Response response = auctionClientService.resolveAuction(selectedAuctionId, AuctionResolutionAction.CANCEL);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                SoundUtil.success();
                NotificationUtil.success("Auction resolved as canceled successfully!");
                loadAuctionDetails(selectedAuctionId);
            } else {
                NotificationUtil.error(response.getMessage() == null ? "Resolution failed." : response.getMessage());
            }
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }
    @FXML
    private void handleSellerCancel() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) return;
        try {
            Response response = auctionClientService.deleteAuction(selectedAuctionId);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                SoundUtil.success();
                NotificationUtil.success("Auction canceled successfully!");
                loadAuctionDetails(selectedAuctionId);
            } else {
                NotificationUtil.error(response.getMessage() == null ? "Auction cancellation failed." : response.getMessage());
            }
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    private void loadSellerReputation(String sellerUsername) {
        if (sellerReputationLabel == null || sellerUsername == null || sellerUsername.isBlank()) return;

        Thread thread = new Thread(() -> {
            try {
                PublicProfileDto profile = publicProfileClientService.getPublicProfile(sellerUsername);
                if (profile != null && profile.getStats() != null) {
                    String stars = profile.getStats().getStarVisual();
                    double rating = profile.getStats().getStarRating();
                    String display = stars + " (" + String.format("%.1f", rating) + ")";
                    Platform.runLater(() -> showSellerReputation(display));
                } else {
                    Platform.runLater(() -> showSellerReputation("★★★★★ (5.0)"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showSellerReputation("★★★★★ (5.0)"));
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void showSellerReputation(String text) {
        if (sellerReputationLabel == null) return;
        sellerReputationLabel.setText(text != null ? text : "★★★★★ (5.0)");
        sellerReputationLabel.setManaged(true);
        sellerReputationLabel.setVisible(true);
    }
}

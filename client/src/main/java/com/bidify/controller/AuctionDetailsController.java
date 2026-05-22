package com.bidify.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.dto.BidDto;
import com.bidify.common.enums.AuctionResolutionAction;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.Event;
import com.bidify.common.model.Response;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.common.utility.TimeUtil;
import com.bidify.common.utility.JsonUtil;
import com.bidify.event.EventManager;
import com.bidify.model.ClientSession;
import com.bidify.service.AuctionClientService;
import com.bidify.service.AuthClientService;
import com.bidify.utility.ImageCache;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;
import com.bidify.utility.UiUpdateScheduler;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class AuctionDetailsController {
    private static final Logger logger = LoggerFactory.getLogger(AuctionDetailsController.class);
    private static final String DEFAULT_PREVIEW_IMAGE = "/images/bidify-logo.png";
    private static final DateTimeFormatter CHART_SHORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CHART_FULL_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter CHART_DAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter CHART_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

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

    private final java.util.List<Image> carouselImages = new java.util.ArrayList<>();
    private int currentCarouselIndex = 0;
    @FXML
    private Label openingBidderLabel;
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
    private double minIncrement;
    private double currentValue;
    private LineChart<Number, Number> biddingChart;
    private NumberAxis biddingTimeAxis;
    private NumberAxis biddingAmountAxis;
    // The gatekeeper calls this first
    // dùng để thiết lập đấu giá ID
    public static void setAuctionId(String auctionId) {
        selectedAuctionId = auctionId;
    }

    // dùng để khởi tạo
    @FXML
    private void initialize() {
        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_UPDATED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleAuctionEnded);
        EventManager.getInstance().subscribe(EventType.AUCTION_DELETED, this::handleAuctionDeleted);
        // dùng để khởi tạo bidding chart
        initializeBiddingChart();

        // Just focus on loading the data for this specific view
        if (selectedAuctionId != null && !selectedAuctionId.isBlank()) {
            // dùng để tải đấu giá thông tin chi tiết
            loadAuctionDetails(selectedAuctionId);
        } else {
            NotificationUtil.error("No auction selected.");
        }
    }

    // dùng để xử lý live cập nhật
    private void handleLiveUpdate(Event event) {
        if (selectedAuctionId == null || event.getData() == null) return;

        AuctionDto updatedAuction = JsonUtil.fromMap(event.getData(), AuctionDto.class);
        if (updatedAuction != null && selectedAuctionId.equals(updatedAuction.getId())) {
            Platform.runLater(() -> {
                updatedAuction.setCurrentUserAutoBidActive(currentUserAutoBidActive);
                updatedAuction.setCurrentUserAutoBidMax(currentUserAutoBidMax);
                // dùng để liên kết dữ liệu đấu giá data
                bindAuctionData(updatedAuction);
                if (event.getType() == EventType.BID_PLACED)
                    NotificationUtil.info(event.getMessage());
            });
        }
    }

    // dùng để xử lý đấu giá ended
    private void handleAuctionEnded(Event event) {
        if (selectedAuctionId == null || event.getData() == null) return;

        AuctionDto endedAuction = JsonUtil.fromMap(event.getData(), AuctionDto.class);
        if (endedAuction != null && selectedAuctionId.equals(endedAuction.getId())) {
            Platform.runLater(() -> {
                // dùng để liên kết dữ liệu đấu giá data
                bindAuctionData(endedAuction);
                placebid.setDisable(true);
                NotificationUtil.info("Auction has ended.");
            });
        }
    }

    // dùng để xử lý đấu giá deleted
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

    // dùng để dọn dẹp tài nguyên
    public void cleanup() {
        // dùng để dừng timer
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

    //bid placing

    // dùng để xử lý place lượt đặt giá
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

            inputprice.clear();
            NotificationUtil.success(response.getMessage() == null ? "Bid placed successfully." : response.getMessage());
            // dùng để tải đấu giá thông tin chi tiết
            loadAuctionDetails(selectedAuctionId);
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    // dùng để xử lý lưu auto lượt đặt giá
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
            // dùng để tải đấu giá thông tin chi tiết
            loadAuctionDetails(selectedAuctionId);
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    // dùng để xử lý disable auto lượt đặt giá
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
            // dùng để tải đấu giá thông tin chi tiết
            loadAuctionDetails(selectedAuctionId);
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            logger.error("Exception occurred", e);
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    // dùng để xử lý selection
    @FXML
    private void handleSelection(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) {
            return;
        }

        if (selectedButton == auctionsButton) {
            missionBarController.setActiveNavigation(auctionsButton);
            // dùng để tomenu
            tomenu();
            return;
        }

        if (selectedButton == createAuctionButton) {
            missionBarController.setActiveNavigation(createAuctionButton);
            // dùng để dọn dẹp tài nguyên
            cleanup();
            SceneManager.switchScene("create-auction.fxml", false, true);
            return;
        }

        if (selectedButton == logoutButton) {
            // dùng để xử lý đăng xuất
            handleLogout();
        }
    }

    //loader

    // dùng để tải đấu giá thông tin chi tiết
    private void loadAuctionDetails(String auctionId) {
        Thread loader = new Thread(() -> {
            try {
                AuctionDto auction = auctionClientService.getAuctionDetail(auctionId);
                // dùng để tham gia đấu giá kênh truyền tải
                joinAuctionChannel(auctionId);
                Platform.runLater(() -> {
                    currentUserAutoBidActive = auction.isCurrentUserAutoBidActive();
                    // dùng để liên kết dữ liệu đấu giá data
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

    // dùng để tham gia đấu giá kênh truyền tải
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

    // dùng để liên kết dữ liệu đấu giá data
    private void bindAuctionData(AuctionDto data) {
        currentAuction = data;
        currentUserAutoBidActive = data.isCurrentUserAutoBidActive();
        currentUserAutoBidMax = data.getCurrentUserAutoBidMax();
        boolean isUpcoming = "UPCOMING".equalsIgnoreCase(data.getStatus());

        //auction name, seller and description
        name.setText(DisplayUtil.defaultText(data.getAuctionName(), "Untitled auction"));
        openingBidderLabel.setText(DisplayUtil.defaultText(data.getSellerUsername(), "Unknown seller"));
        description.setText(DisplayUtil.defaultText(data.getDescription(), "No description."));

        // bid values and date
        double startingValue = data.getStartingPrice();
        this.currentValue= data.getCurrentBid();
        this.minIncrement = data.getMinIncrement();
        currentDisplayedPrice = currentValue > 0 ? currentValue : startingValue;
        
        openingBidAmountLabel.setText(DisplayUtil.formatCashSuffix(startingValue));
        fullStartingPriceLabel.setText(DisplayUtil.formatCurrency(startingValue));
        currentprice.setText(DisplayUtil.formatCashSuffix(currentDisplayedPrice));
        fullCurrentPriceLabel.setText(DisplayUtil.formatCurrency(currentDisplayedPrice));
        opendate.setText(DisplayUtil.formatDateTime(data.getStartTime(), "Unknown"));
        // dùng để configure đấu giá state
        configureAuctionState(data.getStatus(), startingValue, currentValue);
        // dùng để bắt đầu timer
        startTimer();

        // dùng để hiển thị recent activity
        renderRecentActivity(data, isUpcoming);
        // dùng để hiển thị bidding trend
        renderBiddingTrend(data, isUpcoming);
        // dùng để refresh auto lượt đặt giá trạng thái nhãn hiển thị
        refreshAutoBidStatusLabel();
        // dùng để hiển thị audience stats
        renderAudienceStats(data, isUpcoming);

        // anti-sniping info
        if (AntiSnipingVisualLabel != null) {
            String currentUser = com.bidify.model.ClientSession.getInstance().getCurrentUsername();
            boolean isAdmin = com.bidify.model.ClientSession.getInstance().isAdmin();
            boolean isSeller = currentUser != null && currentUser.equals(data.getSellerUsername());
            boolean hasConfig = data.getAntiSnipingTriggerTime() != null && data.getAntiSnipingExtensionTime() != null 
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
        // Populate carousel images
        carouselImages.clear();
        currentCarouselIndex = 0;
        }

        if (data.getThumbnailBase64() != null && !data.getThumbnailBase64().isBlank()) {
            String cacheKey = "auction_" + data.getId() + "_thumb";
            Image img = ImageCache.getInstance().get(cacheKey, data.getThumbnailBase64());
            if (img != null && !img.isError()) {
                carouselImages.add(img);
            }
        }

        if (data.getGalleryBase64() != null) {
            int index = 0;
            for (String base64 : data.getGalleryBase64()) {
                if (base64 != null && !base64.isBlank()) {
                    String cacheKey = "auction_" + data.getId() + "_gallery_" + index++;
                    Image img = ImageCache.getInstance().get(cacheKey, base64);
                    if (img != null && !img.isError() && !carouselImages.contains(img)) {
                        carouselImages.add(img);
                    }
                }
            }
        }

        // set primary image
        if (data.getThumbnailBase64() != null)
            setPreviewImageFromBase64(data.getThumbnailBase64());
        else
            // dùng để thiết lập preview hình ảnh
            setPreviewImage(DEFAULT_PREVIEW_IMAGE);
        // Display current image and update navigation controls
        updateCarouselDisplay();


    }

    // dùng để configure đấu giá state
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

        boolean showSellerCancel = isActive && (isSeller || isAdmin);
        if (sellerCancelButton != null) {
            sellerCancelButton.setManaged(showSellerCancel);
            sellerCancelButton.setVisible(showSellerCancel);
        }

        if (settlementActionSection != null) {
            boolean showSettlementSection = false;
            boolean showPay = false;
            boolean showConfirm = false;
            boolean showAdminComplete = false;
            boolean showAdminCancel = false;
            String statusText = "";

            if ("AWAITING_PAYMENT".equalsIgnoreCase(status)) {
                showSettlementSection = true;
                statusText = "Status: AWAITING PAYMENT (Winner: " + currentAuction.getCurrentBidderUsername() + ")";
                if (currentUsername != null && currentUsername.equals(currentAuction.getCurrentBidderUsername())) {
                    showPay = true;
                }
                if (isAdmin) {
                    showAdminCancel = true;
                }
            } else if ("AWAITING_DELIVERY".equalsIgnoreCase(status)) {
                showSettlementSection = true;
                statusText = "Status: AWAITING DELIVERY";
                if (isSeller || isAdmin) {
                    showConfirm = true;
                }
                if (isAdmin) {
                    showAdminComplete = true;
                    showAdminCancel = true;
                }
            } else if ("COMPLETED".equalsIgnoreCase(status)) {
                showSettlementSection = true;
                statusText = "Status: COMPLETED (Winner: " + currentAuction.getCurrentBidderUsername() + " paid and received)";
            }

            settlementActionSection.setManaged(showSettlementSection);
            settlementActionSection.setVisible(showSettlementSection);
            if (showSettlementSection) {
                settlementStatusLabel.setText(statusText);
                payNowButton.setManaged(showPay);
                payNowButton.setVisible(showPay);
                confirmDeliveryButton.setManaged(showConfirm);
                confirmDeliveryButton.setVisible(showConfirm);
                adminCompleteButton.setManaged(showAdminComplete);
                adminCompleteButton.setVisible(showAdminComplete);
                adminCancelButton.setManaged(showAdminCancel);
                adminCancelButton.setVisible(showAdminCancel);
            }
        }
    }

    // dùng để hiển thị audience stats
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


    // dùng để đặt lại giao diện
    private void resetView() {
        // dùng để dừng timer
        stopTimer();
        currentAuction = null;
        name.setText("Loading auction...");
        description.setText("Please wait while the auction details are fetched.");

        openingBidAmountLabel.setText("Loading...");
        currentprice.setText("Loading...");
        leftMetricLabel.setText("STARTING PRICE");
        rightMetricLabel.setText("CURRENT BID");
        openDateLabel.setText("Open at:");
        endDateLabel.setText("End at:");
        recentActivityLabel.setText("RECENT ACTIVITY");
        bidActionSection.setManaged(true);
        bidActionSection.setVisible(true);

        enddate.setText("Loading...");
        activityList.getChildren().clear();
        // dùng để hiển thị bidding chart state
        showBiddingChartState("Loading bid history...");
        updateBiddingTrendDetail("$0.00", "Loading bid trend...", "neutral");

        openingBidderLabel.setText("Starting price");
        opendate.setText("Opening bid");

        carouselImages.clear();
        currentCarouselIndex = 0;
        if (prevImageButton != null) {
            prevImageButton.setVisible(false);
            prevImageButton.setManaged(false);
        }
        if (nextImageButton != null) {
            nextImageButton.setVisible(false);
            nextImageButton.setManaged(false);
        }
        if (thumbnailContainer != null) {
            thumbnailContainer.getChildren().clear();
            thumbnailContainer.setVisible(false);
            thumbnailContainer.setManaged(false);
        }

        currentDisplayedPrice = 0;
        placebid.setDisable(true);
        currentUserAutoBidActive = false;
        // dùng để refresh auto lượt đặt giá trạng thái nhãn hiển thị
        refreshAutoBidStatusLabel();
    }

    // dùng để refresh auto lượt đặt giá trạng thái nhãn hiển thị
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

    // dùng để định dạng auto lượt đặt giá value
    private String formatAutoBidValue(double value) {
        if (Math.rint(value) == value) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    // dùng để định dạng count
    private String formatCount(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
    }

    // dùng để bắt đầu timer
    private void startTimer() {
        // dùng để dừng timer
        stopTimer();
        if (currentAuction == null) return;

        timerSubscriptionId = UiUpdateScheduler.getInstance().subscribe(this::refreshTimerText);
    }

    // dùng để dừng timer
    private void stopTimer() {
        if (timerSubscriptionId == null || timerSubscriptionId.isBlank())
            return;

        UiUpdateScheduler.getInstance().unsubscribe(timerSubscriptionId);
        timerSubscriptionId = null;
    }

    // dùng để refresh timer text
    private void refreshTimerText() {
        if (currentAuction == null) {
            enddate.setText("Unknown");
            return;
        }

        boolean isUpcoming = "UPCOMING".equalsIgnoreCase(currentAuction.getStatus());
        String targetTime = isUpcoming ? currentAuction.getStartTime() : currentAuction.getEndTime();
        enddate.setText(DisplayUtil.formatRemainingTime(targetTime));
    }

    // dùng để khởi tạo bidding chart
    private void initializeBiddingChart() {
        if (biddingChartHost == null) {
            return;
        }

        biddingTimeAxis = new NumberAxis();
        biddingAmountAxis = new NumberAxis();

        biddingTimeAxis.setLabel("Bid time");
        biddingTimeAxis.setForceZeroInRange(false);
        biddingTimeAxis.setMinorTickVisible(false);

        biddingAmountAxis.setLabel("Bid amount");
        biddingAmountAxis.setForceZeroInRange(false);
        biddingAmountAxis.setMinorTickVisible(false);
        biddingAmountAxis.setTickLabelFormatter(new StringConverter<>() {
            // dùng để chuyển thành string
            @Override
            public String toString(Number value) {
                return DisplayUtil.formatCurrency(value.doubleValue());
            }

            // dùng để từ string
            @Override
            public Number fromString(String string) {
                return 0;
            }
        });

        biddingChart = new LineChart<>(biddingTimeAxis, biddingAmountAxis);
        biddingChart.setAnimated(false);
        biddingChart.setLegendVisible(false);
        biddingChart.setCreateSymbols(true);
        biddingChart.setAlternativeRowFillVisible(false);
        biddingChart.setAlternativeColumnFillVisible(false);
        biddingChart.setHorizontalGridLinesVisible(true);
        biddingChart.setVerticalGridLinesVisible(false);
        biddingChart.setMinHeight(200.0);
        biddingChart.setPrefHeight(200.0);
        biddingChart.getStyleClass().add("bidding-line-chart");

        biddingChartHost.getChildren().setAll(biddingChart);
        // dùng để hiển thị bidding chart state
        showBiddingChartState("Loading bid history...");
    }

    // dùng để hiển thị bidding trend
    private void renderBiddingTrend(AuctionDto data, boolean isUpcoming) {
        if (analyticsSection == null || biddingChart == null) {
            return;
        }

        analyticsSection.setManaged(true);
        analyticsSection.setVisible(true);

        if (isUpcoming) {
            biddingChart.getData().clear();
            // dùng để hiển thị bidding chart state
            showBiddingChartState("Chart appears after first live bid.");
            updateBiddingTrendDetail("$0.00", "Waiting for opening bid.", "neutral");
            return;
        }

        List<BidDto> bidHistory = data.getBidHistory();
        if (bidHistory == null || bidHistory.isEmpty()) {
            biddingChart.getData().clear();
            // dùng để hiển thị bidding chart state
            showBiddingChartState("No bids yet. First live bid will appear here.");
            updateBiddingTrendDetail(DisplayUtil.formatCurrency(data.getCurrentBid()), "No bid movement yet.", "neutral");
            return;
        }

        List<BidDto> sortedBids = bidHistory.stream()
                .sorted(Comparator.comparing(this::parseBidCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        LocalDateTime firstBidTime = parseBidCreatedAt(sortedBids.getFirst());
        LocalDateTime lastBidTime = parseBidCreatedAt(sortedBids.getLast());
        biddingTimeAxis.setTickLabelFormatter(createTimeAxisFormatter(firstBidTime, lastBidTime));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (BidDto bid : sortedBids) {
            series.getData().add(createBidPoint(bid));
        }

        biddingChart.getData().setAll(series);
        updateBiddingTrendDetail(data, sortedBids);
        if (series.getNode() != null) {
            series.getNode().getStyleClass().add("bidding-line-series");
        }
        // dùng để ẩn bidding chart state
        hideBiddingChartState();
    }

    private void updateBiddingTrendDetail(AuctionDto data, List<BidDto> sortedBids) {
        BidDto latestBid = sortedBids.getLast();
        if (sortedBids.size() < 2) {
            updateBiddingTrendDetail(
                DisplayUtil.formatCurrency(latestBid.getAmount()),
                "New bid activity | 1 bid | " + (latestBid.isAutoBidGenerated() ? "AutoBid" : "Manual") + " latest",
                "success"
            );
            return;
        }

        double baseline = data.getStartingPrice() > 0 ? data.getStartingPrice() : sortedBids.getFirst().getAmount();
        double latestAmount = latestBid.getAmount();
        double percentChange = baseline == 0.0 ? 0.0 : ((latestAmount - baseline) / baseline) * 100.0;
        String trendStyle = percentChange >= 0.0 ? "success" : "danger";
        String trendWord = percentChange >= 0.0 ? "↑ Increased" : "↓ Decreased";
        String bidType = latestBid.isAutoBidGenerated() ? "AutoBid" : "Manual";
        String trendText = String.format(
            "%s %+.1f%% from start | %d %s | %s latest",
            trendWord,
            percentChange,
            sortedBids.size(),
            sortedBids.size() == 1 ? "bid" : "bids",
            bidType
        );

        updateBiddingTrendDetail(DisplayUtil.formatCurrency(latestAmount), trendText, trendStyle);
    }

    private void updateBiddingTrendDetail(String metric, String trend, String trendStyle) {
        if (biddingTrendMetricLabel != null) {
            biddingTrendMetricLabel.setText(metric);
        }
        if (biddingTrendChangeLabel != null) {
            biddingTrendChangeLabel.setText(trend);
            biddingTrendChangeLabel.getStyleClass().removeAll(
                "analytics-trend-success",
                "analytics-trend-danger",
                "analytics-trend-neutral"
            );
            biddingTrendChangeLabel.getStyleClass().add("analytics-trend-" + trendStyle);
        }
    }

    private XYChart.Data<Number, Number> createBidPoint(BidDto bid) {
        XYChart.Data<Number, Number> point = new XYChart.Data<>(toEpochSeconds(parseBidCreatedAt(bid)), bid.getAmount());
        point.setNode(createBidPointNode(bid));
        return point;
    }

    // dùng để tạo lượt đặt giá point node
    private Node createBidPointNode(BidDto bid) {
        StackPane node = new StackPane();
        node.getStyleClass().addAll("chart-line-symbol", bid.isAutoBidGenerated() ? "bid-point-auto" : "bid-point-manual");
        node.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        node.setPrefSize(10, 10);
        node.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        node.setPickOnBounds(true);
        attachTooltip(node, createDetailedTooltip(buildBidTooltipText(bid)));
        return node;
    }

    // dùng để build lượt đặt giá tooltip text
    private String buildBidTooltipText(BidDto bid) {
        String bidType = bid.isAutoBidGenerated() ? "AutoBid" : "Manual bid";
        return "Bid Detail"
                + "\nBidder: " + DisplayUtil.defaultText(bid.getBidderUsername(), "Unknown bidder")
                + "\nAmount: " + DisplayUtil.formatCurrency(bid.getAmount())
                + "\nTime: " + DisplayUtil.formatDateTime(bid.getCreatedAt(), "Unknown time")
                + "\nType: " + bidType
                + "\nBid ID: " + DisplayUtil.defaultText(bid.getId(), "Unknown");
    }

    // dùng để tạo detailed tooltip
    private Tooltip createDetailedTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.getStyleClass().add("chart-tooltip");
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }

    // dùng để attach tooltip
    private void attachTooltip(Node node, Tooltip tooltip) {
        node.setOnMouseEntered(event -> tooltip.show(node, event.getScreenX() + 14, event.getScreenY() + 14));
        node.setOnMouseMoved(event -> {
            if (tooltip.isShowing()) {
                tooltip.setAnchorX(event.getScreenX() + 14);
                tooltip.setAnchorY(event.getScreenY() + 14);
            }
        });
        node.setOnMouseExited(event -> tooltip.hide());
    }

    // dùng để hiển thị bidding chart state
    private void showBiddingChartState(String message) {
        if (biddingChartHost != null) {
            biddingChartHost.setManaged(false);
            biddingChartHost.setVisible(false);
        }
        if (biddingChartStateLabel != null) {
            biddingChartStateLabel.setText(message);
            biddingChartStateLabel.setManaged(true);
            biddingChartStateLabel.setVisible(true);
        }
    }

    // dùng để ẩn bidding chart state
    private void hideBiddingChartState() {
        if (biddingChartHost != null) {
            biddingChartHost.setManaged(true);
            biddingChartHost.setVisible(true);
        }
        if (biddingChartStateLabel != null) {
            biddingChartStateLabel.setManaged(false);
            biddingChartStateLabel.setVisible(false);
        }
    }

    // dùng để tạo thời gian axis formatter
    private StringConverter<Number> createTimeAxisFormatter(LocalDateTime firstBidTime, LocalDateTime lastBidTime) {
        DateTimeFormatter formatter = resolveTimeAxisFormatter(firstBidTime, lastBidTime);

        return new StringConverter<>() {
            // dùng để chuyển thành string
            @Override
            public String toString(Number value) {
                LocalDateTime dateTime = fromEpochSeconds(value.longValue());
                return dateTime == null
                        ? ""
                        : dateTime.format(formatter);
            }

            // dùng để từ string
            @Override
            public Number fromString(String string) {
                return 0;
            }
        };
    }

    // dùng để giải quyết thời gian axis formatter
    private DateTimeFormatter resolveTimeAxisFormatter(LocalDateTime firstBidTime, LocalDateTime lastBidTime) {
        if (firstBidTime == null || lastBidTime == null) {
            return CHART_SHORT_TIME_FORMATTER;
        }

        long totalHours = Math.abs(ChronoUnit.HOURS.between(firstBidTime, lastBidTime));
        long totalDays = Math.abs(ChronoUnit.DAYS.between(firstBidTime.toLocalDate(), lastBidTime.toLocalDate()));

        if (totalHours < 24) {
            return CHART_SHORT_TIME_FORMATTER;
        }
        if (totalDays <= 14) {
            return CHART_DAY_FORMATTER;
        }
        if (totalDays <= 90) {
            return CHART_FULL_TIME_FORMATTER;
        }
        return CHART_YEAR_FORMATTER;
    }

    // dùng để phân tích cú pháp lượt đặt giá created tại
    private LocalDateTime parseBidCreatedAt(BidDto bid) {
        if (bid == null || bid.getCreatedAt() == null || bid.getCreatedAt().isBlank()) {
            return null;
        }

        try {
            return TimeUtil.parseDateTime(bid.getCreatedAt());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // dùng để chuyển thành epoch seconds
    private long toEpochSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return TimeUtil.toVietnamEpochSeconds(dateTime);
    }

    // dùng để từ epoch seconds
    private LocalDateTime fromEpochSeconds(long epochSeconds) {
        try {
            return TimeUtil.fromVietnamEpochSeconds(epochSeconds);
        } catch (Exception e) {
            return null;
        }
    }

    // dùng để hiển thị recent activity
    private void renderRecentActivity(AuctionDto data, boolean isUpcoming) {
        if (activityList == null) return;

        activityList.getChildren().clear();
        if (isUpcoming) {
            activityList.getChildren().add(createActivityRow("Bidding opens when the auction goes live.", "", ""));
            return;
        }

        List<BidDto> bidHistory = data.getBidHistory();
        if (bidHistory == null || bidHistory.isEmpty()) {
            activityList.getChildren().add(createActivityRow("No bids placed yet.", "", ""));
            return;
        }

        for (BidDto bid : bidHistory) {
            String bidderText = bid.isAutoBidGenerated()
                    ? DisplayUtil.defaultText(bid.getBidderUsername(), "Unknown bidder") + " (AutoBid)"
                    : DisplayUtil.defaultText(bid.getBidderUsername(), "Unknown bidder");
            activityList.getChildren().add(createActivityRow(
                    bidderText,
                    DisplayUtil.formatCashSuffix(bid.getAmount()),
                    DisplayUtil.formatDateTime(bid.getCreatedAt(), "Unknown")
            ));
        }
    }

    // dùng để tạo activity dòng hiển thị
    private GridPane createActivityRow(String bidderText, String amountText, String timeText) {
        GridPane row = new GridPane();
        row.getStyleClass().add("activity-row");
        row.getColumnConstraints().addAll(
                createActivityColumn(34.0),
                createActivityColumn(33.0),
                createActivityColumn(33.0)
        );

        Label bidderLabel = new Label(bidderText);
        bidderLabel.getStyleClass().add("bidder-name");
        bidderLabel.setWrapText(true);

        Label amountLabel = new Label(amountText);
        amountLabel.getStyleClass().addAll("bidder-name", "right");
        amountLabel.setMaxWidth(Double.MAX_VALUE);

        Label timeLabel = new Label(timeText);
        timeLabel.getStyleClass().addAll("bidder-name", "right");
        timeLabel.setMaxWidth(Double.MAX_VALUE);

        row.add(bidderLabel, 0, 0);
        row.add(amountLabel, 1, 0);
        row.add(timeLabel, 2, 0);
        return row;
    }

    // dùng để tạo activity column
    private ColumnConstraints createActivityColumn(double percentWidth) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percentWidth);
        return column;
    }

    // top bar handlers and miscellaneous

    // dùng để thiết lập preview hình ảnh từ base64
    private void setPreviewImageFromBase64(String base64) {
        if (selectedAuctionId != null && base64 != null) {
            String cacheKey = "auction_" + selectedAuctionId + "_thumb";
            Image cachedImage = ImageCache.getInstance().get(cacheKey, base64);
            if (cachedImage != null) {
                previewimage.setImage(cachedImage);
                return;
            }
        }
        // dùng để thiết lập preview hình ảnh
        setPreviewImage(DEFAULT_PREVIEW_IMAGE);
    }

    // dùng để thiết lập preview hình ảnh
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

    // dùng để update carousel display
    private void updateCarouselDisplay() {
        if (carouselImages.isEmpty()) {
            setPreviewImage(DEFAULT_PREVIEW_IMAGE);
            if (prevImageButton != null) {
                prevImageButton.setVisible(false);
                prevImageButton.setManaged(false);
            }
            if (nextImageButton != null) {
                nextImageButton.setVisible(false);
                nextImageButton.setManaged(false);
            }
            renderThumbnails();
            return;
        }

        if (currentCarouselIndex < 0) {
            currentCarouselIndex = carouselImages.size() - 1;
        } else if (currentCarouselIndex >= carouselImages.size()) {
            currentCarouselIndex = 0;
        }

        previewimage.setImage(carouselImages.get(currentCarouselIndex));

        boolean showNavigation = carouselImages.size() > 1;
        if (prevImageButton != null) {
            prevImageButton.setVisible(showNavigation);
            prevImageButton.setManaged(showNavigation);
        }
        if (nextImageButton != null) {
            nextImageButton.setVisible(showNavigation);
            nextImageButton.setManaged(showNavigation);
        }

        renderThumbnails();
    }

    // dùng để handle prev image
    @FXML
    private void handlePrevImage() {
        currentCarouselIndex--;
        updateCarouselDisplay();
    }

    // dùng để handle next image
    @FXML
    private void handleNextImage() {
        currentCarouselIndex++;
        updateCarouselDisplay();
    }

    // dùng để hiển thị hình ảnh thumbnails
    private void renderThumbnails() {
        if (thumbnailContainer == null) return;
        thumbnailContainer.getChildren().clear();

        if (carouselImages.size() <= 1) {
            thumbnailContainer.setVisible(false);
            thumbnailContainer.setManaged(false);
            return;
        }

        thumbnailContainer.setVisible(true);
        thumbnailContainer.setManaged(true);

        for (int i = 0; i < carouselImages.size(); i++) {
            final int index = i;
            Image img = carouselImages.get(i);

            StackPane thumbPane = new StackPane();
            thumbPane.getStyleClass().add("thumb-card");
            thumbPane.setPrefSize(80, 80);

            // Highlight selected thumbnail
            if (i == currentCarouselIndex) {
                thumbPane.setStyle("-fx-border-color: #00458f; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-background-radius: 6px;");
            } else {
                thumbPane.setStyle("-fx-border-color: #d8e3fb; -fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px;");
            }

            ImageView thumbView = new ImageView(img);
            thumbView.setFitHeight(72);
            thumbView.setFitWidth(72);
            thumbView.setPreserveRatio(true);
            thumbView.setSmooth(true);

            thumbPane.getChildren().add(thumbView);
            thumbPane.setOnMouseClicked(e -> {
                currentCarouselIndex = index;
                updateCarouselDisplay();
            });
            thumbPane.setStyle(thumbPane.getStyle() + " -fx-cursor: hand;");

            thumbnailContainer.getChildren().add(thumbPane);
        }
    }

    // dùng để xử lý đăng xuất
    @FXML
    private void handleLogout() {
        String currentUsername = com.bidify.network.SocketClient.getClient().getCurrentUsername();

        if (currentUsername == null || currentUsername.isBlank()) {
            // dùng để dọn dẹp tài nguyên
            cleanup();
            SceneManager.clearAllCache();
            SceneManager.switchScene("login.fxml", true, false);
            return;
        }

        try {
            Response response = authClientService.logout();
            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Logged out successfully.");
                // dùng để dọn dẹp tài nguyên
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

    // dùng để tomenu
    @FXML
    private void tomenu() {
        // dùng để dọn dẹp tài nguyên
        cleanup();
        SceneManager.switchScene("hub.fxml", false, true);
    }

    @FXML
    private void handlePayNow() {
        if (selectedAuctionId == null || selectedAuctionId.isBlank()) return;
        try {
            Response response = auctionClientService.payAuction(selectedAuctionId);
            if (response.getStatus() == RequestStatus.SUCCESS) {
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
}

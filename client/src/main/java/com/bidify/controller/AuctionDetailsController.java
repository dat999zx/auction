package com.bidify.controller;

import java.io.IOException;
import java.time.Instant;
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
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.Event;
import com.bidify.common.model.Response;
import com.bidify.common.utility.DisplayUtil;
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
import javafx.util.Duration;
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
    private GridPane thumbnailGrid;
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
    private VBox analyticsSection;
    @FXML
    private StackPane biddingChartHost;
    @FXML
    private Label biddingChartStateLabel;

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
    public static void setAuctionId(String auctionId) {
        selectedAuctionId = auctionId;
    }

    @FXML
    private void initialize() {
        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_UPDATED, this::handleLiveUpdate);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleAuctionEnded);
        EventManager.getInstance().subscribe(EventType.AUCTION_DELETED, this::handleAuctionDeleted);
        initializeBiddingChart();

        // Just focus on loading the data for this specific view
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
                if (event.getType() == EventType.BID_PLACED)
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
            NotificationUtil.error("Your bid must be higher than " + DisplayUtil.formatCurrency(currentDisplayedPrice) + minIncrement + ".");
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

    //loader

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
            // Being already joined should not block the detail screen.
        }
    }

    //binding

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
        configureAuctionState(isUpcoming, startingValue, currentValue);
        startTimer();

        renderRecentActivity(data, isUpcoming);
        renderBiddingTrend(data, isUpcoming);
        refreshAutoBidStatusLabel();
        renderAudienceStats(data, isUpcoming);

        // set primary image
        if (data.getThumbnailBase64() != null)
            setPreviewImageFromBase64(data.getThumbnailBase64());
        else
            setPreviewImage(DEFAULT_PREVIEW_IMAGE);

        // setup gallery
        setupThumbnailGallery(data);
    }

    private void configureAuctionState(boolean isUpcoming, double startingValue, double currentValue) {
        if (isUpcoming || ClientSession.getInstance().isAdmin()) {
            leftMetricLabel.setText("STARTING PRICE");
            rightMetricLabel.setText(isUpcoming ? "OPENING BID" : "CURRENT BID");
            currentprice.setText(DisplayUtil.formatCashSuffix(isUpcoming ? startingValue : (currentValue > 0 ? currentValue : startingValue)));
            openDateLabel.setText(isUpcoming ? "Starts at:" : "Open at:");
            endDateLabel.setText("Ends at:");
            recentActivityLabel.setText(isUpcoming ? "AUCTION STATUS" : "RECENT ACTIVITY");
            bidActionSection.setManaged(false);
            bidActionSection.setVisible(false);
            placebid.setDisable(true);
            inputprice.clear();
            return;
        }

        leftMetricLabel.setText("STARTING PRICE");
        rightMetricLabel.setText("CURRENT BID");
        currentprice.setText(DisplayUtil.formatCashSuffix(currentValue > 0 ? currentValue : startingValue));
        openDateLabel.setText("Open at:");
        endDateLabel.setText("End at:");
        recentActivityLabel.setText("RECENT ACTIVITY");
        bidActionSection.setManaged(true);
        bidActionSection.setVisible(true);
        placebid.setDisable(false);
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
        showBiddingChartState("Loading bid history...");

        openingBidderLabel.setText("Starting price");
        opendate.setText("Opening bid");

        if (thumbnailGrid != null) thumbnailGrid.getChildren().clear();

        currentDisplayedPrice = 0;
        placebid.setDisable(true);
        currentUserAutoBidActive = false;
        refreshAutoBidStatusLabel();
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
            @Override
            public String toString(Number value) {
                return DisplayUtil.formatCurrency(value.doubleValue());
            }

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
        showBiddingChartState("Loading bid history...");
    }

    private void renderBiddingTrend(AuctionDto data, boolean isUpcoming) {
        if (analyticsSection == null || biddingChart == null) {
            return;
        }

        analyticsSection.setManaged(true);
        analyticsSection.setVisible(true);

        if (isUpcoming) {
            biddingChart.getData().clear();
            showBiddingChartState("Chart appears after first live bid.");
            return;
        }

        List<BidDto> bidHistory = data.getBidHistory();
        if (bidHistory == null || bidHistory.isEmpty()) {
            biddingChart.getData().clear();
            showBiddingChartState("No bids yet. First live bid will appear here.");
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
        if (series.getNode() != null) {
            series.getNode().getStyleClass().add("bidding-line-series");
        }
        hideBiddingChartState();
    }

    private XYChart.Data<Number, Number> createBidPoint(BidDto bid) {
        XYChart.Data<Number, Number> point = new XYChart.Data<>(toEpochSeconds(parseBidCreatedAt(bid)), bid.getAmount());
        point.setNode(createBidPointNode(bid));
        return point;
    }

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

    private String buildBidTooltipText(BidDto bid) {
        String bidType = bid.isAutoBidGenerated() ? "AutoBid" : "Manual bid";
        return "Bid Detail"
                + "\nBidder: " + DisplayUtil.defaultText(bid.getBidderUsername(), "Unknown bidder")
                + "\nAmount: " + DisplayUtil.formatCurrency(bid.getAmount())
                + "\nTime: " + DisplayUtil.formatDateTime(bid.getCreatedAt(), "Unknown time")
                + "\nType: " + bidType
                + "\nBid ID: " + DisplayUtil.defaultText(bid.getId(), "Unknown");
    }

    private Tooltip createDetailedTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.getStyleClass().add("chart-tooltip");
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }

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

    private StringConverter<Number> createTimeAxisFormatter(LocalDateTime firstBidTime, LocalDateTime lastBidTime) {
        DateTimeFormatter formatter = resolveTimeAxisFormatter(firstBidTime, lastBidTime);

        return new StringConverter<>() {
            @Override
            public String toString(Number value) {
                LocalDateTime dateTime = fromEpochSeconds(value.longValue());
                return dateTime == null
                        ? ""
                        : dateTime.format(formatter);
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        };
    }

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

    private LocalDateTime parseBidCreatedAt(BidDto bid) {
        if (bid == null || bid.getCreatedAt() == null || bid.getCreatedAt().isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(bid.getCreatedAt());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private long toEpochSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private LocalDateTime fromEpochSeconds(long epochSeconds) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
        } catch (Exception e) {
            return null;
        }
    }

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

    private ColumnConstraints createActivityColumn(double percentWidth) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percentWidth);
        return column;
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

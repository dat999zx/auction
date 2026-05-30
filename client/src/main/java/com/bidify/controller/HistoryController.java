package com.bidify.controller;

import java.io.IOException;
import java.util.List;

import com.bidify.common.dto.BidDto;
import com.bidify.common.dto.TransactionDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Event;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.controller.history.BiddingRowController;
import com.bidify.controller.history.EmptyCardController;
import com.bidify.controller.history.TransactionCardController;
import com.bidify.event.EventManager;
import com.bidify.service.BidClientService;
import com.bidify.service.TransactionClientService;
import com.bidify.navigation.CleanableController;
import com.bidify.ui.HistoryChartRenderer;
import com.bidify.navigation.MissionBarUtil;
import com.bidify.navigation.NavPage;
import com.bidify.ui.NotificationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class HistoryController implements CleanableController {
    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);

    @FXML
    private VBox biddingActivityContainer;

    @FXML
    private VBox transactionRecordsContainer;

    @FXML
    private StackPane moneyFlowChartHost;

    @FXML
    private Label moneyFlowChartStateLabel;

    @FXML
    private Label moneyFlowMetricLabel;

    @FXML
    private Label moneyFlowTrendLabel;

    @FXML
    private StackPane biddingActivityChartHost;

    @FXML
    private Label biddingActivityChartStateLabel;

    @FXML
    private Label biddingActivityMetricLabel;

    @FXML
    private Label biddingActivityTrendLabel;

    private final BidClientService bidClientService = new BidClientService();
    private final TransactionClientService transactionClientService = new TransactionClientService();

    private HistoryChartRenderer historyChartRenderer;

    @FXML
    private void initialize() {
        bindTopBar();
        historyChartRenderer = new HistoryChartRenderer(
                moneyFlowChartHost,
                moneyFlowChartStateLabel,
                moneyFlowMetricLabel,
                moneyFlowTrendLabel,
                biddingActivityChartHost,
                biddingActivityChartStateLabel,
                biddingActivityMetricLabel,
                biddingActivityTrendLabel);
        historyChartRenderer.initialize();
        renderLoadingState();

        EventManager.getInstance().subscribe(EventType.WALLET_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().subscribe(EventType.LOCKED_BALANCE_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleRefreshEvent);
        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleRefreshEvent);

        loadHistory();
    }

    // Hủy đăng ký lắng nghe sự kiện khi rời màn hình để tránh rò rỉ bộ nhớ.
    public void cleanup() {
        EventManager.getInstance().unsubscribe(EventType.WALLET_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().unsubscribe(EventType.LOCKED_BALANCE_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_ENDED, this::handleRefreshEvent);
        EventManager.getInstance().unsubscribe(EventType.BID_PLACED, this::handleRefreshEvent);
    }

    private void handleRefreshEvent(Event event) {
        loadHistory();
    }

    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.HISTORY, false, null, this::cleanup);
    }

    private void loadHistory() {
        Thread loaderThread = new Thread(() -> {
            try {
                List<BidDto> bids = bidClientService.getBidHistory();
                List<TransactionDto> transactions = transactionClientService.getTransactionHistory();
                Platform.runLater(() -> renderHistory(bids, transactions));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    renderErrorState("Cannot connect to server.");
                    NotificationUtil.error("Cannot connect to server.");
                });
            } catch (ValidationException e) {
                Platform.runLater(() -> renderErrorState(e.getMessage()));
            }
        });
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void renderLoadingState() {
        if (historyChartRenderer != null)
            historyChartRenderer.renderLoadingState();

        renderBiddingActivity(List.of());
        renderTransactionRecords(List.of());
    }

    private void renderErrorState(String message) {
        if (historyChartRenderer != null)
            historyChartRenderer.renderErrorState(message);

        renderBiddingActivity(List.of());
        renderTransactionRecords(List.of());
        transactionRecordsContainer.getChildren().add(loadEmptyCard(message));
    }

    private void renderHistory(List<BidDto> bids, List<TransactionDto> transactions) {
        if (historyChartRenderer != null)
            historyChartRenderer.render(bids, transactions);

        renderBiddingActivity(bids);
        renderTransactionRecords(transactions);
    }


    private void renderBiddingActivity(List<BidDto> bids) {
        biddingActivityContainer.getChildren().clear();
        biddingActivityContainer.getChildren().add(createBiddingHeader());

        if (bids.isEmpty()) {
            biddingActivityContainer.getChildren().add(
                loadBiddingRow("No bids placed yet.", "Your bidding activity will appear here.", "-", "-", "PENDING", null)
            );
            return;
        }

        for (BidDto bid : bids) {
            biddingActivityContainer.getChildren().add(loadBiddingRow(
                buildAuctionItemLabel(bid),
                buildAuctionSubtitle(bid),
                DisplayUtil.formatCashSuffix(bid.getAmount()),
                DisplayUtil.formatDateTime(bid.getCreatedAt(), "Unknown"),
                buildAuctionStatus(bid),
                bid.getAuctionId()
            ));
        }
    }

    private void renderTransactionRecords(List<TransactionDto> transactions) {
        transactionRecordsContainer.getChildren().clear();

        if (transactions.isEmpty()) {
            transactionRecordsContainer.getChildren().add(loadEmptyCard("No transaction records yet."));
            return;
        }

        for (TransactionDto transaction : transactions) {
            transactionRecordsContainer.getChildren().add(loadTransactionCard(transaction));
        }
    }

    private HBox createBiddingHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("table-header");
        header.getChildren().addAll(
            createHeaderLabel("AUCTION ITEM", 320),
            createHeaderLabel("BID AMOUNT", 180),
            createHeaderLabel("DATE & TIME", 240)
        );

        Label statusHeader = new Label("STATUS");
        statusHeader.getStyleClass().add("table-header-text");
        statusHeader.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statusHeader, Priority.ALWAYS);
        header.getChildren().add(statusHeader);

        return header;
    }

    private Label createHeaderLabel(String text, double prefWidth) {
        Label label = new Label(text);
        label.setPrefWidth(prefWidth);
        label.getStyleClass().add("table-header-text");
        return label;
    }

    private Node loadBiddingRow(String title, String subtitle, String amount, String dateTime, String status, String auctionId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history/bidding-row.fxml"));
            Node node = loader.load();
            BiddingRowController controller = loader.getController();
            controller.setData(title, subtitle, amount, dateTime, status, auctionId);
            return node;
        } catch (IOException e) {
            logger.error("Error loading bidding row", e);
            return new Label("Error loading row");
        }
    }

    private Node loadTransactionCard(TransactionDto transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history/transaction-card.fxml"));
            Node node = loader.load();
            TransactionCardController controller = loader.getController();
            controller.setData(transaction);
            return node;
        } catch (IOException e) {
            logger.error("Error loading transaction card", e);
            return new Label("Error loading card");
        }
    }

    private Node loadEmptyCard(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history/empty-card.fxml"));
            Node node = loader.load();
            EmptyCardController controller = loader.getController();
            controller.setMessage(message);
            return node;
        } catch (IOException e) {
            logger.error("Error loading empty card", e);
            return new Label(message);
        }
    }

    private String buildAuctionItemLabel(BidDto bid) {
        if (bid.getAuctionId() != null && !bid.getAuctionId().isBlank())
            return "Auction " + bid.getAuctionId();
        return "Auction bid";
    }

    private String buildAuctionSubtitle(BidDto bid) {
        return bid.isAutoBidGenerated()
            ? "AutoBid placed"
            : "Manual bid placed";
    }

    private String buildAuctionStatus(BidDto bid) {
        return bid.isAutoBidGenerated() ? "AUTO" : "PLACED";
    }

}

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

import com.bidify.common.dto.BidDto;
import com.bidify.common.dto.TransactionDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Event;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.controller.history.BiddingRowController;
import com.bidify.controller.history.EmptyCardController;
import com.bidify.controller.history.TransactionCardController;
import com.bidify.event.EventManager;
import com.bidify.service.BidClientService;
import com.bidify.service.TransactionClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

public class HistoryController {
    private static final DateTimeFormatter CHART_SHORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CHART_FULL_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM HH:mm");
    private static final DateTimeFormatter CHART_DAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter CHART_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final ZoneId CHART_ZONE_ID = ZoneId.systemDefault();

    @FXML
    private VBox biddingActivityContainer;

    @FXML
    private VBox transactionRecordsContainer;

    @FXML
    private StackPane moneyFlowChartHost;

    @FXML
    private Label moneyFlowChartStateLabel;

    @FXML
    private StackPane biddingActivityChartHost;

    @FXML
    private Label biddingActivityChartStateLabel;

    private final BidClientService bidClientService = new BidClientService();
    private final TransactionClientService transactionClientService = new TransactionClientService();

    private NumberAxis moneyFlowTimeAxis;
    private NumberAxis moneyFlowAmountAxis;
    private LineChart<Number, Number> moneyFlowChart;

    private NumberAxis biddingActivityTimeAxis;
    private NumberAxis biddingActivityCountAxis;
    private LineChart<Number, Number> biddingActivityChart;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            bindTopBar();
            initializeCharts();
            renderLoadingState();
        });

        EventManager.getInstance().subscribe(EventType.WALLET_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().subscribe(EventType.LOCKED_BALANCE_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleRefreshEvent);
        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleRefreshEvent);

        loadHistory();
    }

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
        showChartState(moneyFlowChartHost, moneyFlowChartStateLabel, "Loading transaction history...");
        showChartState(biddingActivityChartHost, biddingActivityChartStateLabel, "Loading bid history...");
        renderBiddingActivity(List.of());
        renderTransactionRecords(List.of());
    }

    private void renderErrorState(String message) {
        showChartState(moneyFlowChartHost, moneyFlowChartStateLabel, message);
        showChartState(biddingActivityChartHost, biddingActivityChartStateLabel, message);
        renderBiddingActivity(List.of());
        renderTransactionRecords(List.of());
        transactionRecordsContainer.getChildren().add(loadEmptyCard(message));
    }

    private void renderHistory(List<BidDto> bids, List<TransactionDto> transactions) {
        renderMoneyFlowTrend(transactions);
        renderBiddingActivityTrend(bids);
        renderBiddingActivity(bids);
        renderTransactionRecords(transactions);
    }

    private void initializeCharts() {
        moneyFlowTimeAxis = createTimeAxis();
        moneyFlowAmountAxis = createCurrencyAxis("Net Flow");
        moneyFlowChart = createAnalyticsChart(moneyFlowTimeAxis, moneyFlowAmountAxis, "money-flow-chart");
        moneyFlowChartHost.getChildren().setAll(moneyFlowChart);

        biddingActivityTimeAxis = createTimeAxis();
        biddingActivityCountAxis = new NumberAxis();
        biddingActivityCountAxis.setAutoRanging(true);
        biddingActivityCountAxis.setForceZeroInRange(true);
        biddingActivityCountAxis.setLabel("Bid Count");
        biddingActivityCountAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return Integer.toString(Math.max(0, value.intValue()));
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
        biddingActivityChart = createAnalyticsChart(biddingActivityTimeAxis, biddingActivityCountAxis, "bidding-activity-chart");
        biddingActivityChartHost.getChildren().setAll(biddingActivityChart);
    }

    private NumberAxis createTimeAxis() {
        NumberAxis timeAxis = new NumberAxis();
        timeAxis.setAutoRanging(true);
        timeAxis.setForceZeroInRange(false);
        timeAxis.setLabel("Time");
        timeAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                LocalDateTime dateTime = fromEpochSeconds(value.longValue());
                return dateTime == null ? "" : dateTime.format(CHART_SHORT_TIME_FORMATTER);
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
        return timeAxis;
    }

    private NumberAxis createCurrencyAxis(String label) {
        NumberAxis amountAxis = new NumberAxis();
        amountAxis.setAutoRanging(true);
        amountAxis.setForceZeroInRange(false);
        amountAxis.setLabel(label);
        amountAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return DisplayUtil.formatCashSuffix(value.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
        return amountAxis;
    }

    private LineChart<Number, Number> createAnalyticsChart(NumberAxis timeAxis, NumberAxis valueAxis, String styleClass) {
        LineChart<Number, Number> chart = new LineChart<>(timeAxis, valueAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);
        chart.setAlternativeRowFillVisible(false);
        chart.setAlternativeColumnFillVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setMinHeight(220.0);
        chart.setPrefHeight(220.0);
        chart.getStyleClass().addAll("analytics-line-chart", styleClass);
        return chart;
    }

    private void renderMoneyFlowTrend(List<TransactionDto> transactions) {
        if (moneyFlowChart == null) {
            return;
        }

        if (transactions == null || transactions.isEmpty()) {
            moneyFlowChart.getData().clear();
            showChartState(moneyFlowChartHost, moneyFlowChartStateLabel, "No transaction history yet.");
            return;
        }

        List<TransactionDto> sortedTransactions = transactions.stream()
            .sorted(Comparator.comparing(this::parseTransactionCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        LocalDateTime firstTime = parseTransactionCreatedAt(sortedTransactions.getFirst());
        LocalDateTime lastTime = parseTransactionCreatedAt(sortedTransactions.getLast());
        moneyFlowTimeAxis.setTickLabelFormatter(createTimeAxisFormatter(firstTime, lastTime));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        double runningTotal = 0.0;
        for (TransactionDto transaction : sortedTransactions) {
            runningTotal += toSignedAmount(transaction);
            series.getData().add(createMoneyFlowPoint(transaction, runningTotal));
        }

        moneyFlowChart.getData().setAll(series);
        hideChartState(moneyFlowChartHost, moneyFlowChartStateLabel);
    }

    private void renderBiddingActivityTrend(List<BidDto> bids) {
        if (biddingActivityChart == null) {
            return;
        }

        if (bids == null || bids.isEmpty()) {
            biddingActivityChart.getData().clear();
            showChartState(biddingActivityChartHost, biddingActivityChartStateLabel, "No bids yet. First live bid will appear here.");
            return;
        }

        List<BidDto> sortedBids = bids.stream()
            .sorted(Comparator.comparing(this::parseBidCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        LocalDateTime firstBidTime = parseBidCreatedAt(sortedBids.getFirst());
        LocalDateTime lastBidTime = parseBidCreatedAt(sortedBids.getLast());
        biddingActivityTimeAxis.setTickLabelFormatter(createTimeAxisFormatter(firstBidTime, lastBidTime));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        int runningCount = 0;
        for (BidDto bid : sortedBids) {
            runningCount++;
            series.getData().add(createBiddingActivityPoint(bid, runningCount));
        }

        biddingActivityChart.getData().setAll(series);
        hideChartState(biddingActivityChartHost, biddingActivityChartStateLabel);
    }

    private void renderBiddingActivity(List<BidDto> bids) {
        biddingActivityContainer.getChildren().clear();
        biddingActivityContainer.getChildren().add(createBiddingHeader());

        if (bids.isEmpty()) {
            biddingActivityContainer.getChildren().add(
                loadBiddingRow("No bids placed yet.", "Your bidding activity will appear here.", "-", "-", "PENDING")
            );
            return;
        }

        for (BidDto bid : bids) {
            biddingActivityContainer.getChildren().add(loadBiddingRow(
                buildAuctionItemLabel(bid),
                buildAuctionSubtitle(bid),
                DisplayUtil.formatCashSuffix(bid.getAmount()),
                DisplayUtil.formatDateTime(bid.getCreatedAt(), "Unknown"),
                buildAuctionStatus(bid)
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

    private Node loadBiddingRow(String title, String subtitle, String amount, String dateTime, String status) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history/bidding-row.fxml"));
            Node node = loader.load();
            BiddingRowController controller = loader.getController();
            controller.setData(title, subtitle, amount, dateTime, status);
            return node;
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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

    private XYChart.Data<Number, Number> createMoneyFlowPoint(TransactionDto transaction, double runningTotal) {
        XYChart.Data<Number, Number> point = new XYChart.Data<>(toEpochSeconds(parseTransactionCreatedAt(transaction)), runningTotal);
        point.nodeProperty().addListener((observable, oldNode, newNode) -> configureMoneyFlowPointNode(newNode, transaction, runningTotal));
        return point;
    }

    private XYChart.Data<Number, Number> createBiddingActivityPoint(BidDto bid, int runningCount) {
        XYChart.Data<Number, Number> point = new XYChart.Data<>(toEpochSeconds(parseBidCreatedAt(bid)), runningCount);
        point.nodeProperty().addListener((observable, oldNode, newNode) -> configureBiddingActivityPointNode(newNode, bid, runningCount));
        return point;
    }

    private void configureMoneyFlowPointNode(Node node, TransactionDto transaction, double runningTotal) {
        if (node == null) {
            return;
        }

        node.getStyleClass().add(isPositiveFlow(transaction) ? "money-flow-point-positive" : "money-flow-point-negative");
        Tooltip.install(node, new Tooltip(buildMoneyFlowTooltipText(transaction, runningTotal)));
    }

    private void configureBiddingActivityPointNode(Node node, BidDto bid, int runningCount) {
        if (node == null) {
            return;
        }

        node.getStyleClass().add(bid.isAutoBidGenerated() ? "bid-activity-point-auto" : "bid-activity-point-manual");
        Tooltip.install(node, new Tooltip(buildBiddingActivityTooltipText(bid, runningCount)));
    }

    private String buildMoneyFlowTooltipText(TransactionDto transaction, double runningTotal) {
        String direction = isPositiveFlow(transaction) ? "Inflow" : "Outflow";
        return buildTransactionLabel(transaction)
            + "\n" + direction + ": " + DisplayUtil.formatCurrency(Math.abs(transaction.getAmount()))
            + "\nNet total: " + DisplayUtil.formatCurrency(runningTotal)
            + "\n" + DisplayUtil.formatDateTime(transaction.getCreatedAt(), "Unknown time");
    }

    private String buildBiddingActivityTooltipText(BidDto bid, int runningCount) {
        String auctionLabel = bid.getAuctionId() == null || bid.getAuctionId().isBlank()
            ? "Auction bid"
            : "Auction " + bid.getAuctionId();
        String bidType = bid.isAutoBidGenerated() ? "AutoBid" : "Manual bid";

        return auctionLabel
            + "\n" + DisplayUtil.defaultText(bid.getBidderUsername(), "Unknown bidder")
            + "\n" + DisplayUtil.formatCurrency(bid.getAmount())
            + "\nBid count: " + runningCount
            + "\n" + DisplayUtil.formatDateTime(bid.getCreatedAt(), "Unknown time")
            + "\n" + bidType;
    }

    private void showChartState(StackPane chartHost, Label stateLabel, String message) {
        if (chartHost != null) {
            chartHost.setManaged(false);
            chartHost.setVisible(false);
        }
        if (stateLabel != null) {
            stateLabel.setText(message);
            stateLabel.setManaged(true);
            stateLabel.setVisible(true);
        }
    }

    private void hideChartState(StackPane chartHost, Label stateLabel) {
        if (chartHost != null) {
            chartHost.setManaged(true);
            chartHost.setVisible(true);
        }
        if (stateLabel != null) {
            stateLabel.setManaged(false);
            stateLabel.setVisible(false);
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

    private LocalDateTime parseTransactionCreatedAt(TransactionDto transaction) {
        if (transaction == null || transaction.getCreatedAt() == null || transaction.getCreatedAt().isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(transaction.getCreatedAt());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String buildTransactionLabel(TransactionDto transaction) {
        if (transaction == null || transaction.getType() == null) {
            return "Transaction";
        }

        return switch (transaction.getType()) {
            case DEPOSIT -> "Deposit";
            case WITHDRAW -> "Withdrawal";
            case AUCTION_PAY -> "Auction payment";
            case AUCTION_PROFIT -> "Auction profit";
        };
    }

    private boolean isPositiveFlow(TransactionDto transaction) {
        if (transaction == null || transaction.getType() == null) {
            return true;
        }

        return transaction.getType() == TransactionType.DEPOSIT
            || transaction.getType() == TransactionType.AUCTION_PROFIT;
    }

    private double toSignedAmount(TransactionDto transaction) {
        if (transaction == null) {
            return 0.0;
        }

        return isPositiveFlow(transaction) ? transaction.getAmount() : -transaction.getAmount();
    }

    private long toEpochSeconds(LocalDateTime dateTime) {
        return dateTime == null ? 0L : dateTime.atZone(CHART_ZONE_ID).toEpochSecond();
    }

    private LocalDateTime fromEpochSeconds(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), CHART_ZONE_ID);
    }
}

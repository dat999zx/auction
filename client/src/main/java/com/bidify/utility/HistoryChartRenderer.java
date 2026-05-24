package com.bidify.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

import com.bidify.common.dto.BidDto;
import com.bidify.common.dto.TransactionDto;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.common.utility.TimeUtil;

import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

public class HistoryChartRenderer {
    private static final DateTimeFormatter CHART_SHORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StackPane moneyFlowChartHost;
    private final Label moneyFlowChartStateLabel;
    private final Label moneyFlowMetricLabel;
    private final Label moneyFlowTrendLabel;

    private final StackPane biddingActivityChartHost;
    private final Label biddingActivityChartStateLabel;
    private final Label biddingActivityMetricLabel;
    private final Label biddingActivityTrendLabel;

    private NumberAxis moneyFlowTimeAxis;
    private NumberAxis moneyFlowAmountAxis;
    private LineChart<Number, Number> moneyFlowChart;

    private NumberAxis biddingActivityTimeAxis;
    private NumberAxis biddingActivityCountAxis;
    private LineChart<Number, Number> biddingActivityChart;

    public HistoryChartRenderer(
            StackPane moneyFlowChartHost,
            Label moneyFlowChartStateLabel,
            Label moneyFlowMetricLabel,
            Label moneyFlowTrendLabel,
            StackPane biddingActivityChartHost,
            Label biddingActivityChartStateLabel,
            Label biddingActivityMetricLabel,
            Label biddingActivityTrendLabel) {
        this.moneyFlowChartHost = moneyFlowChartHost;
        this.moneyFlowChartStateLabel = moneyFlowChartStateLabel;
        this.moneyFlowMetricLabel = moneyFlowMetricLabel;
        this.moneyFlowTrendLabel = moneyFlowTrendLabel;
        this.biddingActivityChartHost = biddingActivityChartHost;
        this.biddingActivityChartStateLabel = biddingActivityChartStateLabel;
        this.biddingActivityMetricLabel = biddingActivityMetricLabel;
        this.biddingActivityTrendLabel = biddingActivityTrendLabel;
    }

    public void initialize() {
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

    public void renderLoadingState() {
        showChartState(moneyFlowChartHost, moneyFlowChartStateLabel, "Loading transaction history...");
        showChartState(biddingActivityChartHost, biddingActivityChartStateLabel, "Loading bid history...");
        updateChartDetail(moneyFlowMetricLabel, moneyFlowTrendLabel, "$0.00", "Loading flow trend...", "neutral");
        updateChartDetail(biddingActivityMetricLabel, biddingActivityTrendLabel, "0 bids", "Loading bid trend...", "neutral");
    }

    public void renderErrorState(String message) {
        showChartState(moneyFlowChartHost, moneyFlowChartStateLabel, message);
        showChartState(biddingActivityChartHost, biddingActivityChartStateLabel, message);
        updateChartDetail(moneyFlowMetricLabel, moneyFlowTrendLabel, "$0.00", message, "danger");
        updateChartDetail(biddingActivityMetricLabel, biddingActivityTrendLabel, "0 bids", message, "danger");
    }

    public void render(List<BidDto> bids, List<TransactionDto> transactions) {
        renderMoneyFlowTrend(transactions);
        renderBiddingActivityTrend(bids);
    }

    private NumberAxis createTimeAxis() {
        NumberAxis timeAxis = new NumberAxis();
        timeAxis.setAutoRanging(true);
        timeAxis.setForceZeroInRange(false);
        timeAxis.setLabel("Time");
        timeAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                LocalDateTime dateTime = ChartRenderUtil.fromEpochSeconds(value.longValue());
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
            updateChartDetail(moneyFlowMetricLabel, moneyFlowTrendLabel, "$0.00", "No money movement yet", "neutral");
            return;
        }

        if (transactions == null || transactions.isEmpty()) {
            moneyFlowChart.getData().clear();
            showChartState(moneyFlowChartHost, moneyFlowChartStateLabel, "No transaction history yet.");
            updateChartDetail(moneyFlowMetricLabel, moneyFlowTrendLabel, "$0.00", "No money movement yet", "neutral");
            return;
        }

        List<TransactionDto> sortedTransactions = transactions.stream()
            .sorted(Comparator.comparing(this::parseTransactionCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        LocalDateTime firstTime = parseTransactionCreatedAt(sortedTransactions.getFirst());
        LocalDateTime lastTime = parseTransactionCreatedAt(sortedTransactions.getLast());
        moneyFlowTimeAxis.setTickLabelFormatter(ChartRenderUtil.createTimeAxisFormatter(firstTime, lastTime));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        double runningTotal = 0.0;
        for (TransactionDto transaction : sortedTransactions) {
            runningTotal += toSignedAmount(transaction);
            series.getData().add(createMoneyFlowPoint(transaction, runningTotal));
        }

        moneyFlowChart.getData().setAll(series);
        updateMoneyFlowDetail(sortedTransactions, runningTotal);
        hideChartState(moneyFlowChartHost, moneyFlowChartStateLabel);
    }

    private void renderBiddingActivityTrend(List<BidDto> bids) {
        if (biddingActivityChart == null) {
            return;
        }

        if (bids == null || bids.isEmpty()) {
            biddingActivityChart.getData().clear();
            showChartState(biddingActivityChartHost, biddingActivityChartStateLabel, "No bids yet. First live bid will appear here.");
            updateChartDetail(biddingActivityMetricLabel, biddingActivityTrendLabel, "0 bids", "No activity yet", "neutral");
            return;
        }

        List<BidDto> sortedBids = bids.stream()
            .sorted(Comparator.comparing(this::parseBidCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        LocalDateTime firstBidTime = parseBidCreatedAt(sortedBids.getFirst());
        LocalDateTime lastBidTime = parseBidCreatedAt(sortedBids.getLast());
        biddingActivityTimeAxis.setTickLabelFormatter(ChartRenderUtil.createTimeAxisFormatter(firstBidTime, lastBidTime));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        int runningCount = 0;
        for (BidDto bid : sortedBids) {
            runningCount++;
            series.getData().add(createBiddingActivityPoint(bid, runningCount));
        }

        biddingActivityChart.getData().setAll(series);
        updateBiddingActivityDetail(sortedBids);
        hideChartState(biddingActivityChartHost, biddingActivityChartStateLabel);
    }

    private void updateMoneyFlowDetail(List<TransactionDto> transactions, double runningTotal) {
        if (transactions.size() < 2) {
            updateChartDetail(
                moneyFlowMetricLabel,
                moneyFlowTrendLabel,
                DisplayUtil.formatCurrency(runningTotal),
                "New money movement",
                "success"
            );
            return;
        }

        int midpoint = Math.max(1, transactions.size() / 2);
        double earlyFlow = transactions.subList(0, midpoint).stream()
            .mapToDouble(this::toSignedAmount)
            .sum();
        double recentFlow = transactions.subList(midpoint, transactions.size()).stream()
            .mapToDouble(this::toSignedAmount)
            .sum();

        boolean improved = recentFlow >= earlyFlow;
        updateChartDetail(
            moneyFlowMetricLabel,
            moneyFlowTrendLabel,
            DisplayUtil.formatCurrency(runningTotal),
            buildTrendText(improved ? "↑ Increased" : "↓ Decreased", earlyFlow, recentFlow, "recent flow"),
            improved ? "success" : "danger"
        );
    }

    private void updateBiddingActivityDetail(List<BidDto> bids) {
        if (bids.size() < 2) {
            BidDto latestBid = bids.getFirst();
            updateChartDetail(
                biddingActivityMetricLabel,
                biddingActivityTrendLabel,
                "1 bid",
                "New bid activity | latest " + DisplayUtil.formatCashSuffix(latestBid.getAmount()),
                "success"
            );
            return;
        }

        int midpoint = Math.max(1, bids.size() / 2);
        int earlyCount = midpoint;
        int recentCount = bids.size() - midpoint;
        BidDto latestBid = bids.getLast();
        boolean improved = recentCount >= earlyCount;
        String trendText = buildTrendText(improved ? "↑ Increased" : "↓ Decreased", earlyCount, recentCount, "recent bids")
            + " | latest " + DisplayUtil.formatCashSuffix(latestBid.getAmount());

        updateChartDetail(
            biddingActivityMetricLabel,
            biddingActivityTrendLabel,
            bids.size() + (bids.size() == 1 ? " bid" : " bids"),
            trendText,
            improved ? "success" : "danger"
        );
    }

    private String buildTrendText(String symbol, double previousValue, double currentValue, String label) {
        if (Math.abs(previousValue) < 0.01) return "New " + label;

        double percentChange = ((currentValue - previousValue) / Math.abs(previousValue)) * 100.0;
        return symbol + " " + String.format("%+.1f%% %s", percentChange, label);
    }

    private void updateChartDetail(Label metricLabel, Label trendLabel, String metric, String trend, String trendStyle) {
        if (metricLabel != null)
            metricLabel.setText(metric);
        
        if (trendLabel != null) {
            trendLabel.setText(trend);
            trendLabel.getStyleClass().removeAll(
                "analytics-trend-success",
                "analytics-trend-danger",
                "analytics-trend-neutral"
            );
            trendLabel.getStyleClass().add("analytics-trend-" + trendStyle);
        }
    }

    private XYChart.Data<Number, Number> createMoneyFlowPoint(TransactionDto transaction, double runningTotal) {
        XYChart.Data<Number, Number> point = new XYChart.Data<>(ChartRenderUtil.toEpochSeconds(parseTransactionCreatedAt(transaction)), runningTotal);
        point.setNode(createMoneyFlowPointNode(transaction, runningTotal));
        return point;
    }

    private XYChart.Data<Number, Number> createBiddingActivityPoint(BidDto bid, int runningCount) {
        XYChart.Data<Number, Number> point = new XYChart.Data<>(ChartRenderUtil.toEpochSeconds(parseBidCreatedAt(bid)), runningCount);
        point.setNode(createBiddingActivityPointNode(bid, runningCount));
        return point;
    }

    private Node createMoneyFlowPointNode(TransactionDto transaction, double runningTotal) {
        StackPane node = new StackPane();
        node.getStyleClass().addAll(
            "chart-line-symbol",
            isPositiveFlow(transaction) ? "money-flow-point-positive" : "money-flow-point-negative"
        );
        node.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        node.setPrefSize(10, 10);
        node.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        node.setPickOnBounds(true);
        ChartRenderUtil.attachTooltip(node, ChartRenderUtil.createDetailedTooltip(buildMoneyFlowTooltipText(transaction, runningTotal)));
        return node;
    }

    private Node createBiddingActivityPointNode(BidDto bid, int runningCount) {
        StackPane node = new StackPane();
        node.getStyleClass().addAll(
            "chart-line-symbol",
            bid.isAutoBidGenerated() ? "bid-activity-point-auto" : "bid-activity-point-manual"
        );
        node.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        node.setPrefSize(10, 10);
        node.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        node.setPickOnBounds(true);
        ChartRenderUtil.attachTooltip(node, ChartRenderUtil.createDetailedTooltip(buildBiddingActivityTooltipText(bid, runningCount)));
        return node;
    }

    private String buildMoneyFlowTooltipText(TransactionDto transaction, double runningTotal) {
        String direction = isPositiveFlow(transaction) ? "Inflow" : "Outflow";
        return "Money Flow Detail"
            + "\nType: " + buildTransactionLabel(transaction)
            + "\nDirection: " + direction
            + "\nAmount: " + DisplayUtil.formatCurrency(Math.abs(transaction.getAmount()))
            + "\nNet total: " + DisplayUtil.formatCurrency(runningTotal)
            + "\nTime: " + DisplayUtil.formatDateTime(transaction.getCreatedAt(), "Unknown time")
            + "\nAuction: " + DisplayUtil.defaultText(transaction.getAuctionId(), "N/A")
            + "\nTransaction ID: " + DisplayUtil.defaultText(transaction.getId(), "Unknown");
    }

    private String buildBiddingActivityTooltipText(BidDto bid, int runningCount) {
        String auctionLabel = bid.getAuctionId() == null || bid.getAuctionId().isBlank()
            ? "Auction bid"
            : "Auction " + bid.getAuctionId();
        String bidType = bid.isAutoBidGenerated() ? "AutoBid" : "Manual bid";

        return "Bidding Activity Detail"
            + "\nAuction: " + auctionLabel
            + "\nBidder: " + DisplayUtil.defaultText(bid.getBidderUsername(), "Unknown bidder")
            + "\nAmount: " + DisplayUtil.formatCurrency(bid.getAmount())
            + "\nBid count: " + runningCount
            + "\nTime: " + DisplayUtil.formatDateTime(bid.getCreatedAt(), "Unknown time")
            + "\nType: " + bidType
            + "\nBid ID: " + DisplayUtil.defaultText(bid.getId(), "Unknown");
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

    private LocalDateTime parseTransactionCreatedAt(TransactionDto transaction) {
        if (transaction == null || transaction.getCreatedAt() == null || transaction.getCreatedAt().isBlank()) {
            return null;
        }

        try {
            return TimeUtil.parseDateTime(transaction.getCreatedAt());
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
            case AUCTION_REFUND -> "Auction refund";
        };
    }

    private boolean isPositiveFlow(TransactionDto transaction) {
        if (transaction == null || transaction.getType() == null) {
            return true;
        }

        return transaction.getType() == TransactionType.DEPOSIT
            || transaction.getType() == TransactionType.AUCTION_PROFIT
            || transaction.getType() == TransactionType.AUCTION_REFUND;
    }

    private double toSignedAmount(TransactionDto transaction) {
        if (transaction == null) return 0.0;

        return isPositiveFlow(transaction) ? transaction.getAmount() : -transaction.getAmount();
    }
}

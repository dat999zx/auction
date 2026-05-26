package com.bidify.utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.dto.BidDto;
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

public class AuctionBiddingChartRenderer {
    private final StackPane biddingChartHost;
    private final Label biddingChartStateLabel;
    private final Label biddingTrendMetricLabel;
    private final Label biddingTrendChangeLabel;

    private LineChart<Number, Number> biddingChart;
    private NumberAxis biddingTimeAxis;
    private NumberAxis biddingAmountAxis;

    public AuctionBiddingChartRenderer(
            StackPane biddingChartHost,
            Label biddingChartStateLabel,
            Label biddingTrendMetricLabel,
            Label biddingTrendChangeLabel) {
        this.biddingChartHost = biddingChartHost;
        this.biddingChartStateLabel = biddingChartStateLabel;
        this.biddingTrendMetricLabel = biddingTrendMetricLabel;
        this.biddingTrendChangeLabel = biddingTrendChangeLabel;
    }

    public void initialize() {
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

    public void render(AuctionDto data, boolean isUpcoming) {
        if (biddingChart == null) {
            return;
        }

        if (isUpcoming) {
            biddingChart.getData().clear();
            showBiddingChartState("Chart appears after first live bid.");
            updateBiddingTrendDetail("$0.00", "Waiting for opening bid.", "neutral");
            return;
        }

        List<BidDto> bidHistory = data.getBidHistory();
        if (bidHistory == null || bidHistory.isEmpty()) {
            biddingChart.getData().clear();
            showBiddingChartState("No bids yet. First live bid will appear here.");
            updateBiddingTrendDetail(DisplayUtil.formatCurrency(data.getCurrentBid()), "No bid movement yet.", "neutral");
            return;
        }

        List<BidDto> sortedBids = bidHistory.stream()
                .sorted(Comparator.comparing(this::parseBidCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        LocalDateTime firstBidTime = parseBidCreatedAt(sortedBids.getFirst());
        LocalDateTime lastBidTime = parseBidCreatedAt(sortedBids.getLast());
        biddingTimeAxis.setTickLabelFormatter(ChartRenderUtil.createTimeAxisFormatter(firstBidTime, lastBidTime));

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (BidDto bid : sortedBids) {
            series.getData().add(createBidPoint(bid));
        }

        biddingChart.getData().setAll(series);
        updateBiddingTrendDetail(data, sortedBids);
        if (series.getNode() != null) {
            series.getNode().getStyleClass().add("bidding-line-series");
        }
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
        XYChart.Data<Number, Number> point = new XYChart.Data<>(ChartRenderUtil.toEpochSeconds(parseBidCreatedAt(bid)), bid.getAmount());
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
        ChartRenderUtil.attachTooltip(node, ChartRenderUtil.createDetailedTooltip(buildBidTooltipText(bid)));
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
}

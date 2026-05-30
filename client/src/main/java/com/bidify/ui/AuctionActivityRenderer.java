package com.bidify.ui;

import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.dto.BidDto;
import com.bidify.common.utility.DisplayUtil;

import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * Renders the recent bid activity list in the auction detail view.
 */
public class AuctionActivityRenderer {

    public void render(VBox activityList, AuctionDto data, boolean isUpcoming) {
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
                createColumn(34.0),
                createColumn(33.0),
                createColumn(33.0)
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

    private ColumnConstraints createColumn(double percentWidth) {
        ColumnConstraints column = new ColumnConstraints();
        column.setPercentWidth(percentWidth);
        return column;
    }
}

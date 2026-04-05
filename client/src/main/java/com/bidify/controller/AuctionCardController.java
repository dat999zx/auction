package com.bidify.controller;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import com.bidify.common.dto.AuctionDto;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class AuctionCardController {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    private Label timerText;
    @FXML
    private Label lotPill;
    @FXML
    private Label title;
    @FXML
    private Label subtitle;
    @FXML
    private Label currentBidValue;
    @FXML
    private Label bidCountValue;
    @FXML
    private Label sellerLabel;
    @FXML
    private Button bidButton;

    private AuctionDto auction;

    public void bind(AuctionDto auction) {
        this.auction = auction;
        timerText.setText(formatRemainingTime(auction.getEndTime()));
        lotPill.setText(defaultText(auction.getId(), "Auction"));
        title.setText(defaultText(auction.getAuctionName(), "Untitled auction"));
        subtitle.setText(defaultText(auction.getDescription(), "No description."));
        currentBidValue.setText(CURRENCY_FORMAT.format(auction.getCurrentBid()));
        bidCountValue.setText(Integer.toString(auction.getBidCount()));
        sellerLabel.setText("Seller: " + defaultText(auction.getSeller(), "Unknown"));
    }

    @FXML
    private void openAuction() {
        if (auction != null) {
            AuctionDetailsController.openAuctionDetails(auction.getId());
        }
    }

    private String formatRemainingTime(String endTime) {
        if (endTime == null || endTime.isBlank()) {
            return "Unknown";
        }
        try {
            Duration duration = Duration.between(LocalDateTime.now(), LocalDateTime.parse(endTime));
            if (duration.isNegative() || duration.isZero()) {
                return "Ended";
            }
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } catch (DateTimeParseException e) {
            return endTime;
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

package com.bidify.controller;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.utility.DisplayUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AuctionCardController {
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
    private Label currentBidder;
    @FXML
    private Label sellerLabel;

    private AuctionDto auction;

    public void bind(AuctionDto auction) {
        this.auction = auction;
        timerText.setText(DisplayUtil.formatRemainingTime(auction.getEndTime()));
        lotPill.setText(DisplayUtil.defaultText(auction.getId(), "Auction"));
        title.setText(DisplayUtil.defaultText(auction.getAuctionName(), "Untitled auction"));
        subtitle.setText(DisplayUtil.defaultText(auction.getDescription(), "No description."));

        //dealing with no one bidded yet case
        if (auction.getCurrentBid() == 0) {
            currentBidValue.setText(DisplayUtil.formatCurrency(auction.getStartingPrice()));
            currentBidder.setText("No bids yet");
        } else {
            currentBidValue.setText(DisplayUtil.formatCurrency(auction.getCurrentBid()));
            currentBidder.setText(auction.getCurrentBidderUsername());
        }
        sellerLabel.setText("Seller: " + DisplayUtil.defaultText(auction.getSellerUsername(), "Unknown"));
    }

    @FXML
    private void openAuction() {
        if (auction != null) AuctionDetailsController.openAuctionDetails(auction.getId());
    }
}

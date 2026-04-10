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
    private Label bidCountValue;
    @FXML
    private Label sellerLabel;

    private AuctionDto auction;

    public void bind(AuctionDto auction) {
        this.auction = auction;
        timerText.setText(DisplayUtil.formatRemainingTime(auction.getEndTime()));
        lotPill.setText(DisplayUtil.defaultText(auction.getId(), "Auction"));
        title.setText(DisplayUtil.defaultText(auction.getAuctionName(), "Untitled auction"));
        subtitle.setText(DisplayUtil.defaultText(auction.getDescription(), "No description."));
        currentBidValue.setText(DisplayUtil.formatCurrency(auction.getCurrentBid()));
        bidCountValue.setText(Integer.toString(auction.getBidCount()));
        sellerLabel.setText("Seller: " + DisplayUtil.defaultText(auction.getSeller(), "Unknown"));
    }

    @FXML
    private void openAuction() {
        if (auction != null) AuctionDetailsController.openAuctionDetails(auction.getId());
    }
}

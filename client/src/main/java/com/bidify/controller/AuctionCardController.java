package com.bidify.controller;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.utility.DisplayUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.util.Base64;

public class AuctionCardController {
    @FXML
    private ImageView auctionImageView;
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

        if (auction.getThumbnailBase64() != null && !auction.getThumbnailBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(auction.getThumbnailBase64());
                auctionImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
            } catch (Exception e) {
                // set về default nếu fail
                auctionImageView.setImage(null);
            }
        } else {
            auctionImageView.setImage(null);
        }
    }

    @FXML
    private void openAuction() {
        if (auction != null) AuctionDetailsController.openAuctionDetails(auction.getId());
    }
}

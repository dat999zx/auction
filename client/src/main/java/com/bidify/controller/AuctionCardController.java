package com.bidify.controller;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.network.SocketClient;
import com.bidify.utility.ImageCache;
import com.bidify.utility.SceneManager;
import com.bidify.utility.UiUpdateScheduler;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AuctionCardController {
    @FXML
    private ImageView auctionImageView;
    @FXML
    private Label statusPill;
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
    private String timerSubscriptionId;

    public void bind(AuctionDto auction) {
        cleanup();
        this.auction = auction;
        String status = DisplayUtil.defaultText(auction.getStatus(), "LIVE").toUpperCase();
        boolean isUpcoming = "UPCOMING".equals(status);

        statusPill.setText(isUpcoming ? "UPCOMING" : "LIVE");
        statusPill.getStyleClass().removeAll("status-pill-live", "status-pill-upcoming");
        statusPill.getStyleClass().add(isUpcoming ? "status-pill-upcoming" : "status-pill-live");
        lotPill.setText(DisplayUtil.defaultText(auction.getId(), "Auction"));
        title.setText(DisplayUtil.defaultText(auction.getAuctionName(), "Untitled auction"));
        subtitle.setText(DisplayUtil.defaultText(auction.getDescription(), "No description."));
        startTimer(status);

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
            String cacheKey = "auction_" + auction.getId() + "_thumb";
            Image cachedImage = ImageCache.getInstance().get(cacheKey, auction.getThumbnailBase64());
            auctionImageView.setImage(cachedImage);
        } else {
            auctionImageView.setImage(null);
        }
    }

    public void cleanup() {
        if (timerSubscriptionId == null || timerSubscriptionId.isBlank())
            return;

        UiUpdateScheduler.getInstance().unsubscribe(timerSubscriptionId);
        timerSubscriptionId = null;
    }

    private void startTimer(String status) {
        boolean isUpcoming = "UPCOMING".equals(status);
        String targetTime = isUpcoming ? auction.getStartTime() : auction.getEndTime();

        timerSubscriptionId = UiUpdateScheduler.getInstance()
                .subscribe(() -> timerText.setText(DisplayUtil.formatRemainingTime(targetTime)));
    }

    @FXML
    private void openAuction() {
        if (auction == null) return;

        String currentUsername = SocketClient.getClient().getCurrentUsername();
        String auctionId = auction.getId();
        String sellerUsername = auction.getSellerUsername();

        // 2. Decide the destination
        if (sellerUsername != null && sellerUsername.equals(currentUsername) && "UPCOMING".equals(auction.getStatus())) {
            // User owns it -> Go to Modify
            ModifyAuctionController.setAuctionId(auctionId);
            SceneManager.clearCache("modifyauction.fxml");
            SceneManager.switchScene("modifyauction.fxml", false, false);
        } else {
            // User is a buyer -> Go to Details
            AuctionDetailsController.setAuctionId(auctionId);
            SceneManager.clearCache("auctiondetail.fxml");
            SceneManager.switchScene("auctiondetail.fxml", false, false);
        }
    }
}

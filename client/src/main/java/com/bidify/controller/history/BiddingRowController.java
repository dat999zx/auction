package com.bidify.controller.history;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class BiddingRowController {
    @FXML
    private HBox row;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label amountLabel;

    @FXML
    private Label dateLabel;

    @FXML
    private Label statusLabel;

    // dùng để thiết lập data
    public void setData(String title, String subtitle, String amount, String dateTime, String status) {
        setData(title, subtitle, amount, dateTime, status, null);
    }

    // dùng để thiết lập data với auctionId
    public void setData(String title, String subtitle, String amount, String dateTime, String status, String auctionId) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
        amountLabel.setText(amount);
        dateLabel.setText(dateTime);
        statusLabel.setText(status);
        
        statusLabel.getStyleClass().clear();
        statusLabel.getStyleClass().add(resolveBadgeStyle(status));

        if (auctionId != null && !auctionId.isBlank()) {
            row.setStyle("-fx-cursor: hand;");
            row.setOnMouseClicked(event -> {
                com.bidify.controller.AuctionDetailsController.setAuctionId(auctionId);
                com.bidify.utility.SceneManager.clearCache("auctiondetail.fxml");
                com.bidify.utility.SceneManager.switchScene("auctiondetail.fxml", false, false);
            });
        } else {
            row.setStyle("");
            row.setOnMouseClicked(null);
        }
    }

    // dùng để giải quyết badge style
    private String resolveBadgeStyle(String status) {
        if ("WON".equals(status) || "PROFIT".equals(status) || "PLACED".equals(status) || "AUTO".equals(status) || "PENDING".equals(status))
            return "badge-won";
        return "badge-lost";
    }
}

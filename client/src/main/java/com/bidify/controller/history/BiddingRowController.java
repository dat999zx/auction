package com.bidify.controller.history;

import com.bidify.navigation.SceneManager;

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

    public void setData(String title, String subtitle, String amount, String dateTime, String status) {
        setData(title, subtitle, amount, dateTime, status, null);
    }

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
                SceneManager.goAuctionDetail(auctionId);
            });
        } else {
            row.setStyle("");
            row.setOnMouseClicked(null);
        }
    }

    private String resolveBadgeStyle(String status) {
        if ("WON".equals(status) || "PROFIT".equals(status) || "PLACED".equals(status) || "AUTO".equals(status) || "PENDING".equals(status))
            return "badge-won";
        return "badge-lost";
    }
}

package com.bidify.controller.history;

import com.bidify.common.dto.TransactionDto;
import com.bidify.common.utility.DisplayUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class TransactionCardController {
    @FXML
    private HBox card;

    @FXML
    private Label iconLabel;

    @FXML
    private Label titleLabel;

    @FXML
    private Label idLabel;

    @FXML
    private Label amountLabel;

    @FXML
    private Label fullValue;

    // dùng để thiết lập data
    public void setData(TransactionDto transaction) {
        titleLabel.setText(resolveTransactionTitle(transaction));
        idLabel.setText("Transaction ID: " + DisplayUtil.defaultText(transaction.getId(), "Unknown"));
        amountLabel.setText(DisplayUtil.formatCashSuffix(transaction.getAmount()));
        fullValue.setText(DisplayUtil.formatCurrency(transaction.getAmount()));
        iconLabel.setText(resolveTransactionIcon(transaction));

        card.getStyleClass().removeAll("border-green", "border-gray", "border-blue");
        card.getStyleClass().add(resolveTransactionBorderStyle(transaction));

        String auctionId = transaction.getAuctionId();
        if (auctionId != null && !auctionId.isBlank()) {
            card.getStyleClass().add("transaction-card-clickable");
            card.setOnMouseClicked(event -> {
                com.bidify.controller.AuctionDetailsController.setAuctionId(auctionId);
                com.bidify.utility.SceneManager.clearCache("auctiondetail.fxml");
                com.bidify.utility.SceneManager.switchScene("auctiondetail.fxml", false, false);
            });
        } else {
            card.getStyleClass().remove("transaction-card-clickable");
            card.setOnMouseClicked(null);
        }
    }

    // dùng để giải quyết giao dịch border style
    private String resolveTransactionBorderStyle(TransactionDto transaction) {
        if (transaction.getType() == null) return "border-gray";
        return switch (transaction.getType()) {
            case DEPOSIT -> "border-green";
            case WITHDRAW -> "border-gray";
            case AUCTION_PAY, AUCTION_PROFIT -> "border-blue";
            case AUCTION_REFUND -> "border-green";
        };
    }

    // dùng để giải quyết giao dịch icon
    private String resolveTransactionIcon(TransactionDto transaction) {
        if (transaction.getType() == null) return "help";
        return switch (transaction.getType()) {
            case DEPOSIT -> "Deposit";
            case WITHDRAW -> "Withdraw";
            case AUCTION_PAY, AUCTION_PROFIT -> "Payment";
            case AUCTION_REFUND -> "Refund";
        };
    }

    // dùng để giải quyết giao dịch title
    private String resolveTransactionTitle(TransactionDto transaction) {
        if (transaction.getType() == null) return "Transaction";
        return switch (transaction.getType()) {
            case DEPOSIT -> "Wallet Deposit";
            case WITHDRAW -> "Wallet Withdraw";
            case AUCTION_PAY -> "Auction Payment";
            case AUCTION_PROFIT -> "Auction Profit";
            case AUCTION_REFUND -> "Auction Refund";
        };
    }
}

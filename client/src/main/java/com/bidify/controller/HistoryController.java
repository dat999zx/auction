package com.bidify.controller;

import java.io.IOException;
import java.util.List;

import com.bidify.common.dto.TransactionDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Event;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.event.EventManager;
import com.bidify.service.TransactionClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class HistoryController {
    @FXML
    private VBox biddingActivityContainer;

    @FXML
    private VBox transactionRecordsContainer;

    private final TransactionClientService transactionClientService = new TransactionClientService();

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            bindTopBar();
            renderLoadingState();
        });

        EventManager.getInstance().subscribe(EventType.WALLET_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().subscribe(EventType.LOCKED_BALANCE_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleRefreshEvent);

        loadHistory();
    }

    public void cleanup() {
        EventManager.getInstance().unsubscribe(EventType.WALLET_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().unsubscribe(EventType.LOCKED_BALANCE_CHANGED, this::handleRefreshEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_ENDED, this::handleRefreshEvent);
    }

    private void handleRefreshEvent(Event event) {
        loadHistory();
    }

    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.HISTORY, false, null, this::cleanup);
    }

    private void loadHistory() {
        Thread loaderThread = new Thread(() -> {
            try {
                List<TransactionDto> transactions = transactionClientService.getTransactionHistory();
                Platform.runLater(() -> renderHistory(transactions));
            } catch (IOException e) {
                Platform.runLater(() -> {
                    renderErrorState("Cannot connect to server.");
                    NotificationUtil.error("Cannot connect to server.");
                });
            } catch (ValidationException e) {
                Platform.runLater(() -> renderErrorState(e.getMessage()));
            }
        });
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void renderLoadingState() {
        renderBiddingActivity(List.of());
        renderTransactionRecords(List.of());
    }

    private void renderErrorState(String message) {
        renderBiddingActivity(List.of());
        renderTransactionRecords(List.of());
        transactionRecordsContainer.getChildren().add(createEmptyCard(message));
    }

    private void renderHistory(List<TransactionDto> transactions) {
        renderBiddingActivity(transactions);
        renderTransactionRecords(transactions);
    }

    private void renderBiddingActivity(List<TransactionDto> transactions) {
        biddingActivityContainer.getChildren().clear();
        biddingActivityContainer.getChildren().add(createBiddingHeader());

        List<TransactionDto> auctionTransactions = transactions.stream()
            .filter(this::isAuctionTransaction)
            .toList();

        if (auctionTransactions.isEmpty()) {
            biddingActivityContainer.getChildren().add(
                createBiddingRow("No auction activity yet.", "Waiting for completed auction activity.", "-", "-", "PENDING")
            );
            return;
        }

        for (TransactionDto transaction : auctionTransactions) {
            biddingActivityContainer.getChildren().add(createBiddingRow(
                buildAuctionItemLabel(transaction),
                buildAuctionSubtitle(transaction),
                DisplayUtil.formatCurrency(transaction.getAmount()),
                DisplayUtil.formatDateTime(transaction.getCreatedAt(), "Unknown"),
                buildAuctionStatus(transaction)
            ));
        }
    }

    private void renderTransactionRecords(List<TransactionDto> transactions) {
        transactionRecordsContainer.getChildren().clear();

        if (transactions.isEmpty()) {
            transactionRecordsContainer.getChildren().add(createEmptyCard("No transaction records yet."));
            return;
        }

        for (TransactionDto transaction : transactions) {
            transactionRecordsContainer.getChildren().add(createTransactionCard(transaction));
        }
    }

    private HBox createBiddingHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("table-header");
        header.getChildren().addAll(
            createHeaderLabel("AUCTION ITEM", 320),
            createHeaderLabel("BID AMOUNT", 180),
            createHeaderLabel("DATE & TIME", 240)
        );

        Label statusHeader = new Label("STATUS");
        statusHeader.getStyleClass().add("table-header-text");
        statusHeader.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statusHeader, Priority.ALWAYS);
        header.getChildren().add(statusHeader);

        return header;
    }

    private Label createHeaderLabel(String text, double prefWidth) {
        Label label = new Label(text);
        label.setPrefWidth(prefWidth);
        label.getStyleClass().add("table-header-text");
        return label;
    }

    private HBox createBiddingRow(String title, String subtitle, String amount, String dateTime, String status) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("table-row");

        HBox itemCell = new HBox(16);
        itemCell.setAlignment(Pos.CENTER_LEFT);
        itemCell.setPrefWidth(320);

        Pane placeholder = new Pane();
        placeholder.setPrefSize(48, 48);
        placeholder.getStyleClass().add("image-placeholder");

        VBox textBox = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().addAll("text-primary", "table-row-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().addAll("text-on-surface-variant", "table-row-subtitle");
        textBox.getChildren().addAll(titleLabel, subtitleLabel);
        itemCell.getChildren().addAll(placeholder, textBox);

        Label amountLabel = new Label(amount);
        amountLabel.setPrefWidth(180);
        amountLabel.getStyleClass().addAll("text-secondary", "table-row-amount");

        Label dateLabel = new Label(dateTime);
        dateLabel.setPrefWidth(240);
        dateLabel.getStyleClass().add("text-on-surface-variant");

        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(statusBox, Priority.ALWAYS);

        Label statusLabel = new Label(status);
        statusLabel.getStyleClass().add(resolveBadgeStyle(status));
        statusBox.getChildren().add(statusLabel);

        row.getChildren().addAll(itemCell, amountLabel, dateLabel, statusBox);
        return row;
    }

    private HBox createTransactionCard(TransactionDto transaction) {
        HBox card = new HBox(24);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("transaction-card", resolveTransactionBorderStyle(transaction));

        HBox left = new HBox(16);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox iconCircle = new HBox();
        iconCircle.setAlignment(Pos.CENTER);
        iconCircle.getStyleClass().add("icon-circle");
        Label iconLabel = new Label(resolveTransactionIcon(transaction));
        iconLabel.getStyleClass().addAll("icon", "text-primary");
        iconCircle.getChildren().add(iconLabel);

        VBox textBox = new VBox(4);
        Label titleLabel = new Label(resolveTransactionTitle(transaction));
        titleLabel.getStyleClass().addAll("text-primary", "transaction-card-title");
        Label idLabel = new Label("Transaction ID: " + DisplayUtil.defaultText(transaction.getId(), "Unknown"));
        idLabel.getStyleClass().addAll("text-on-surface-variant", "transaction-card-id");
        textBox.getChildren().addAll(titleLabel, idLabel);

        left.getChildren().addAll(iconCircle, textBox);

        VBox amountBox = new VBox(4);
        amountBox.setAlignment(Pos.CENTER_RIGHT);
        Label amountHeader = new Label("AMOUNT");
        amountHeader.getStyleClass().add("table-header-text");
        Label amountLabel = new Label(DisplayUtil.formatCurrency(transaction.getAmount()));
        amountLabel.getStyleClass().addAll("amount-text", "text-primary");
        amountBox.getChildren().addAll(amountHeader, amountLabel);

        card.getChildren().addAll(left, amountBox);
        return card;
    }

    private HBox createEmptyCard(String message) {
        HBox card = new HBox();
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("transaction-card", "border-gray");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("text-on-surface-variant");
        card.getChildren().add(messageLabel);
        return card;
    }

    private boolean isAuctionTransaction(TransactionDto transaction) {
        return transaction.getType() == TransactionType.AUCTION_PAY
            || transaction.getType() == TransactionType.AUCTION_PROFIT;
    }

    private String buildAuctionItemLabel(TransactionDto transaction) {
        if (transaction.getAuctionId() != null && !transaction.getAuctionId().isBlank())
            return "Auction " + transaction.getAuctionId();
        return "Auction activity";
    }

    private String buildAuctionSubtitle(TransactionDto transaction) {
        return switch (transaction.getType()) {
            case AUCTION_PAY -> "Winning bidder payment recorded";
            case AUCTION_PROFIT -> "Seller payout recorded";
            default -> "Auction settlement";
        };
    }

    private String buildAuctionStatus(TransactionDto transaction) {
        return switch (transaction.getType()) {
            case AUCTION_PAY -> "WON";
            case AUCTION_PROFIT -> "PROFIT";
            default -> "DONE";
        };
    }

    private String resolveBadgeStyle(String status) {
        if ("WON".equals(status) || "PROFIT".equals(status))
            return "badge-won";
        return "badge-lost";
    }

    private String resolveTransactionBorderStyle(TransactionDto transaction) {
        return switch (transaction.getType()) {
            case DEPOSIT -> "border-green";
            case WITHDRAW -> "border-gray";
            case AUCTION_PAY, AUCTION_PROFIT -> "border-blue";
        };
    }

    private String resolveTransactionIcon(TransactionDto transaction) {
        return switch (transaction.getType()) {
            case DEPOSIT -> "Deposit";
            case WITHDRAW -> "Withdraw";
            case AUCTION_PAY, AUCTION_PROFIT -> "Payment";
        };
    }

    private String resolveTransactionTitle(TransactionDto transaction) {
        return switch (transaction.getType()) {
            case DEPOSIT -> "Wallet Deposit";
            case WITHDRAW -> "Wallet Withdraw";
            case AUCTION_PAY -> "Auction Payment";
            case AUCTION_PROFIT -> "Auction Profit";
        };
    }
}

package com.bidify.controller;

import java.io.IOException;
import java.util.List;

import com.bidify.common.dto.TransactionDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Event;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.controller.history.BiddingRowController;
import com.bidify.controller.history.EmptyCardController;
import com.bidify.controller.history.TransactionCardController;
import com.bidify.event.EventManager;
import com.bidify.service.TransactionClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
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
        transactionRecordsContainer.getChildren().add(loadEmptyCard(message));
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
                loadBiddingRow("No auction activity yet.", "Waiting for completed auction activity.", "-", "-", "PENDING")
            );
            return;
        }

        for (TransactionDto transaction : auctionTransactions) {
            biddingActivityContainer.getChildren().add(loadBiddingRow(
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
            transactionRecordsContainer.getChildren().add(loadEmptyCard("No transaction records yet."));
            return;
        }

        for (TransactionDto transaction : transactions) {
            transactionRecordsContainer.getChildren().add(loadTransactionCard(transaction));
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

    private Node loadBiddingRow(String title, String subtitle, String amount, String dateTime, String status) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history/bidding-row.fxml"));
            Node node = loader.load();
            BiddingRowController controller = loader.getController();
            controller.setData(title, subtitle, amount, dateTime, status);
            return node;
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading row");
        }
    }

    private Node loadTransactionCard(TransactionDto transaction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history/transaction-card.fxml"));
            Node node = loader.load();
            TransactionCardController controller = loader.getController();
            controller.setData(transaction);
            return node;
        } catch (IOException e) {
            e.printStackTrace();
            return new Label("Error loading card");
        }
    }

    private Node loadEmptyCard(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history/empty-card.fxml"));
            Node node = loader.load();
            EmptyCardController controller = loader.getController();
            controller.setMessage(message);
            return node;
        } catch (IOException e) {
            e.printStackTrace();
            return new Label(message);
        }
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
}

package com.bidify.controller;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import com.bidify.common.dto.UserDto;
import com.bidify.common.dto.WalletRequestDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Event;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.common.utility.JsonUtil;
import com.bidify.event.EventManager;
import com.bidify.model.ClientSession;
import com.bidify.service.UserProfileClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class WalletController {
    private final ClientSession clientSession = ClientSession.getInstance();
    private final UserProfileClientService userProfileClientService = new UserProfileClientService();

    @FXML
    private Label walletBalanceLabel;

    @FXML
    private Label lockedBalanceLabel;

    @FXML
    private Label fullWalletBalance;

    @FXML
    private Label fullLockedBalance;

    @FXML
    private TextField topUpAmountField;

    @FXML
    private TextField withdrawAmountField;

    @FXML
    private VBox walletControlsBox;

    @FXML
    private VBox requestsContainer;

    @FXML
    private Label requestsSummaryLabel;

    private final Consumer<Event> onWalletChanged = e -> Platform.runLater(() -> refreshWalletFromEvent(e));
    private final Consumer<Event> onLockedBalanceChanged = e -> Platform.runLater(() -> refreshWalletFromEvent(e));
    private final Consumer<Event> onWalletRequestsChanged = e -> Platform.runLater(this::loadRequests);

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            MissionBarUtil.setup(NavPage.WALLET, false, null, this::cleanup);
            toggleWalletControls(!clientSession.isAdmin());
            loadWallet();
        });

        EventManager.getInstance().subscribe(EventType.WALLET_CHANGED, onWalletChanged);
        EventManager.getInstance().subscribe(EventType.LOCKED_BALANCE_CHANGED, onLockedBalanceChanged);
        EventManager.getInstance().subscribe(EventType.WALLET_REQUESTS_CHANGED, onWalletRequestsChanged);
    }

    private void cleanup() {
        EventManager.getInstance().unsubscribe(EventType.WALLET_CHANGED, onWalletChanged);
        EventManager.getInstance().unsubscribe(EventType.LOCKED_BALANCE_CHANGED, onLockedBalanceChanged);
        EventManager.getInstance().unsubscribe(EventType.WALLET_REQUESTS_CHANGED, onWalletRequestsChanged);
    }

    @FXML
    private void handleRefresh() {
        loadWallet();
    }

    @FXML
    private void handleTopUp() {
        try {
            userProfileClientService.addWalletBalance(parseAmount(topUpAmountField.getText(), "Deposit amount"));
            topUpAmountField.clear();
            NotificationUtil.success("Deposit request submitted - pending admin approval.");
            loadRequests();
        }
        // catch ValidationException before IOException as it's a subtype of RuntimeException, but let's be explicit
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
    }

    @FXML
    private void handleWithdraw() {
        try {
            userProfileClientService.withdrawWalletBalance(parseAmount(withdrawAmountField.getText(), "Withdraw amount"));
            withdrawAmountField.clear();
            NotificationUtil.success("Withdraw request submitted - pending admin approval.");
            loadWallet();
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
    }

    private void loadWallet() {
        try {
            renderWallet(userProfileClientService.getCurrentProfile());
        } catch (IOException e) {
            renderWallet(userProfileClientService.getCachedProfile());
            NotificationUtil.error("Cannot connect to server.");
        } catch (ValidationException e) {
            renderWallet(userProfileClientService.getCachedProfile());
            NotificationUtil.error(e.getMessage());
        }

        loadRequests();
    }

    private void loadRequests() {
        if (requestsContainer == null) return;
        try {
            List<WalletRequestDto> requests = userProfileClientService.getUserWalletRequests();
            renderRequests(requests);
        } catch (Exception e) {
            requestsContainer.getChildren().clear();
            requestsSummaryLabel.setText("Cannot load requests");
            Label failed = new Label("Failed to load requests.");
            failed.getStyleClass().add("section-copy");
            requestsContainer.getChildren().add(failed);
        }
    }

    private void renderWallet(UserDto user) {
        if (user == null || user.getWallet() == null) {
            walletBalanceLabel.setText("$0.00");
            fullWalletBalance.setText("$0.00");
            lockedBalanceLabel.setText("$0.00");
            fullLockedBalance.setText("$0.00");
            return;
        }

        walletBalanceLabel.setText(DisplayUtil.formatCashSuffix(user.getWallet().getBalance()));
        fullWalletBalance.setText(DisplayUtil.formatCurrency(user.getWallet().getBalance()));
        lockedBalanceLabel.setText(DisplayUtil.formatCashSuffix(user.getWallet().getLockedBalance()));
        fullLockedBalance.setText(DisplayUtil.formatCurrency(user.getWallet().getLockedBalance()));
    }

    private void renderRequests(List<WalletRequestDto> requests) {
        requestsContainer.getChildren().clear();
        if (requests == null || requests.isEmpty()) {
            requestsSummaryLabel.setText("No requests");
            Label empty = new Label("No request history.");
            empty.getStyleClass().add("section-copy");
            requestsContainer.getChildren().add(empty);
            return;
        }

        requestsSummaryLabel.setText("Showing " + requests.size() + " request" + (requests.size() == 1 ? "" : "s"));
        for (WalletRequestDto req : requests) {
            requestsContainer.getChildren().add(createRequestRow(req));
        }
    }

    private HBox createRequestRow(WalletRequestDto req) {
        HBox row = new HBox(12);
        row.getStyleClass().add("wallet-req-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox details = new VBox(3);
        HBox.setHgrow(details, Priority.ALWAYS);

        String reqTypeStr = req.getType() == TransactionType.DEPOSIT ? "Deposit" : "Withdraw";
        Label title = new Label(reqTypeStr + " - $" + String.format("%.2f", req.getAmount()));
        title.getStyleClass().add("wallet-req-title");

        String created = req.getCreatedAt() != null && req.getCreatedAt().length() >= 16
            ? req.getCreatedAt().substring(0, 16).replace('T', ' ')
            : DisplayUtil.defaultText(req.getCreatedAt(), "Unknown date");
        Label sub = new Label("Submitted " + created);
        sub.getStyleClass().add("wallet-req-sub");

        details.getChildren().addAll(title, sub);

        String statusStyle = switch (req.getStatus()) {
            case PENDING -> "wallet-req-status-pending";
            case APPROVED -> "wallet-req-status-approved";
            case DENIED -> "wallet-req-status-denied";
        };
        Label status = new Label(req.getStatus().name());
        status.getStyleClass().addAll("wallet-req-status", statusStyle);

        row.getChildren().addAll(details, status);
        return row;
    }

    private void refreshWalletFromEvent(Event event) {
        if (event != null && event.getData() != null) {
            UserDto updatedUser = JsonUtil.fromMap(event.getData(), UserDto.class);
            if (updatedUser != null) {
                renderWallet(updatedUser);
                return;
            }
        }
        loadWallet();
    }

    private double parseAmount(String rawValue, String fieldName) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.isBlank()) {
            throw new ValidationException(fieldName + " cannot be empty");
        }

        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number");
        }
    }

    private void toggleWalletControls(boolean visible) {
        if (walletControlsBox == null) return;
        walletControlsBox.setManaged(visible);
        walletControlsBox.setVisible(visible);
    }
}

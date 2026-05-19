package com.bidify.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.EventType;
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
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class UserProfileController {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);
    private final ClientSession clientSession = ClientSession.getInstance();

    @FXML
    private Label usernameValueLabel;

    @FXML
    private Label walletBalanceLabel;

    @FXML
    private Label lockedBalanceLabel;

    @FXML
    private Label fullWalletBalance;

    @FXML
    private Label fullLockedBalance;

    @FXML
    private Label memberStatusLabel;

    @FXML
    private Label profileImageHintLabel;

    @FXML
    private Label profileAvatarLabel;

    @FXML
    private TextField nicknameField;

    @FXML
    private TextField topUpAmountField;

    @FXML
    private TextField withdrawAmountField;

    @FXML
    private PasswordField currentPasswordField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    private final UserProfileClientService userProfileClientService = new UserProfileClientService();

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            bindTopBar();
            populateProfile();
        });

        EventManager.getInstance().subscribe(EventType.WALLET_CHANGED, this::handleWalletChanged);
        EventManager.getInstance().subscribe(EventType.LOCKED_BALANCE_CHANGED, this::handleLockedBalanceChanged);
        EventManager.getInstance().subscribe(EventType.SERVER_NOTICE, this::handleServerNotice);
    }

    private void handleWalletChanged(Event event) {
        Platform.runLater(() -> refreshProfileFromEvent(event));
    }

    private void handleLockedBalanceChanged(Event event) {
        Platform.runLater(() -> refreshProfileFromEvent(event));
    }

    private void handleServerNotice(Event event) {
        Platform.runLater(() -> NotificationUtil.info(event.getMessage()));
    }

    public void cleanup() {
        EventManager.getInstance().unsubscribe(EventType.WALLET_CHANGED, this::handleWalletChanged);
        EventManager.getInstance().unsubscribe(EventType.LOCKED_BALANCE_CHANGED, this::handleLockedBalanceChanged);
        EventManager.getInstance().unsubscribe(EventType.SERVER_NOTICE, this::handleServerNotice);
    }

    @FXML
    private void handleSaveProfile() {
        try {
            UserDto updatedUser = userProfileClientService.updateProfile(nicknameField.getText());
            refreshProfile(updatedUser);
            NotificationUtil.success("Profile updated successfully.");
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void handleTopUp() {
        try {
            UserDto updatedUser = userProfileClientService.addWalletBalance(parseAmount(topUpAmountField.getText(), "Deposit amount"));
            topUpAmountField.clear();
            refreshProfile(updatedUser);
            NotificationUtil.success("Wallet updated successfully.");
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void handleWithdraw() {
        try {
            UserDto updatedUser = userProfileClientService.withdrawWalletBalance(parseAmount(withdrawAmountField.getText(), "Withdraw amount"));
            withdrawAmountField.clear();
            refreshProfile(updatedUser);
            NotificationUtil.success("Wallet updated successfully.");
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void handleChangePassword() {
        try {
            userProfileClientService.changePassword(
                currentPasswordField.getText(),
                newPasswordField.getText(),
                confirmPasswordField.getText()
            );
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            NotificationUtil.success("Password updated successfully.");
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    @FXML
    private void handleProfileImagePlaceholder() {
        NotificationUtil.info("Profile image upload is a UI placeholder right now.");
    }

    private void populateProfile() {
        try {
            refreshProfile(userProfileClientService.getCurrentProfile());
        } catch (IOException e) {
            refreshProfile(userProfileClientService.getCachedProfile());
            NotificationUtil.error("Cannot connect to server.");
        } catch (ValidationException e) {
            refreshProfile(userProfileClientService.getCachedProfile());
            NotificationUtil.error(e.getMessage());
        }

        profileImageHintLabel.setText("Profile image upload placeholder");
    }

    private void refreshProfileFromEvent(Event event) {
        if (event != null && event.getData() != null) {
            UserDto updatedUser = JsonUtil.fromMap(event.getData(), UserDto.class);
            if (updatedUser != null) {
                refreshProfile(updatedUser);
                return;
            }
        }
        populateProfile();
    }

    private void refreshProfile(UserDto user) {
        usernameValueLabel.setText(DisplayUtil.defaultText(user.getUsername(), "Unknown"));
        nicknameField.setText(DisplayUtil.defaultText(user.getNickname(), user.getUsername()));
        walletBalanceLabel.setText(DisplayUtil.formatCashSuffix(user.getWallet().getBalance()));
        fullWalletBalance.setText(DisplayUtil.formatCurrency(user.getWallet().getBalance()));

        lockedBalanceLabel.setText(DisplayUtil.formatCashSuffix(user.getWallet().getLockedBalance()));
        fullLockedBalance.setText(DisplayUtil.formatCurrency(user.getWallet().getLockedBalance()));
        
        memberStatusLabel.setText(clientSession.isAdmin() ? "Administrator" : "Active bidder");
        String avatarLetter = resolveAvatarLetter(user.getNickname(), user.getUsername());
        profileAvatarLabel.setText(avatarLetter);
        toggleWalletControls(!clientSession.isAdmin());
        
        var controller = SceneManager.getMissionBarController();
        if (controller != null) {
            controller.setAvatarText(avatarLetter);
        }
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

    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.PROFILE, false, null, this::cleanup);
    }

    private String resolveAvatarLetter(String nickname, String username) {
        String source = nickname;
        if (source == null || source.isBlank()) {
            source = username;
        }
        if (source == null || source.isBlank()) {
            return "U";
        }
        return source.substring(0, 1).toUpperCase();
    }

    private void toggleWalletControls(boolean visible) {
        if (topUpAmountField != null && topUpAmountField.getParent() != null && topUpAmountField.getParent().getParent() != null) {
            topUpAmountField.getParent().getParent().setManaged(visible);
            topUpAmountField.getParent().getParent().setVisible(visible);
        }
        if (withdrawAmountField != null && withdrawAmountField.getParent() != null && withdrawAmountField.getParent().getParent() != null) {
            withdrawAmountField.getParent().getParent().setManaged(visible);
            withdrawAmountField.getParent().getParent().setVisible(visible);
        }
    }
}

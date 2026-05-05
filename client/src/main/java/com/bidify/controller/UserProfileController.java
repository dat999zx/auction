package com.bidify.controller;

import java.io.IOException;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Event;
import com.bidify.common.model.Response;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.event.EventManager;
import com.bidify.service.AuthClientService;
import com.bidify.service.UserProfileClientService;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserProfileController {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);
    @FXML
    private Button auctionsButton;

    @FXML
    private Button createAuctionButton;

    @FXML
    private Label messageLabel;

    @FXML
    private Label usernameValueLabel;

    @FXML
    private Label walletBalanceLabel;

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

    private MissionBarController missionBarController;

    private final AuthClientService authClientService = new AuthClientService();
    private final UserProfileClientService userProfileClientService = new UserProfileClientService();

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            bindTopBar();
            populateProfile();
        });

        EventManager.getInstance().subscribe(EventType.WALLET_CHANGED, this::handleWalletChanged);
        EventManager.getInstance().subscribe(EventType.SERVER_NOTICE, this::handleServerNotice);
    }

    private void handleWalletChanged(Event event) {
        Platform.runLater(this::populateProfile);
    }

    private void handleServerNotice(Event event) {
        Platform.runLater(() -> showMessage(event.getMessage(), true));
    }

    public void cleanup() {
        EventManager.getInstance().unsubscribe(EventType.WALLET_CHANGED, this::handleWalletChanged);
        EventManager.getInstance().unsubscribe(EventType.SERVER_NOTICE, this::handleServerNotice);
    }

    @FXML
    private void handleSelection(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) {
            return;
        }

        if (selectedButton == auctionsButton) {
            cleanup();
            SceneManager.switchScene("hub.fxml", false, true);
            return;
        }

        if (selectedButton == createAuctionButton) {
            cleanup();
            SceneManager.switchScene("create-auction.fxml", false, true);
        }
    }

    @FXML
    private void handleLogout() {
        String currentUsername = com.bidify.network.SocketClient.getClient().getCurrentUsername();

        if (currentUsername == null || currentUsername.isBlank()) {
            cleanup();
            SceneManager.clearAllCache();
            SceneManager.switchScene("login.fxml", true, false);
            return;
        }

        try {
            Response response = authClientService.logout();
            if (response.getStatus() == RequestStatus.SUCCESS) {
                cleanup();
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml", true, false);
                return;
            }
            showMessage(response.getMessage(), false);
        }
        catch (IOException e) {
            showMessage("Cannot connect to server.", false);
            logger.error("Exception occurred", e);
        }
        catch (com.bidify.common.exception.AuthException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void handleSaveProfile() {
        try {
            UserDto updatedUser = userProfileClientService.updateProfile(nicknameField.getText());
            refreshProfile(updatedUser);
            showMessage("Profile updated successfully.", true);
        }
        catch (IOException e) {
            showMessage("Cannot connect to server.", false);
        }
        catch (ValidationException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void handleTopUp() {
        try {
            UserDto updatedUser = userProfileClientService.addWalletBalance(parseAmount(topUpAmountField.getText(), "Top up amount"));
            topUpAmountField.clear();
            refreshProfile(updatedUser);
            showMessage("Wallet updated successfully.", true);
        }
        catch (IOException e) {
            showMessage("Cannot connect to server.", false);
        }
        catch (ValidationException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void handleWithdraw() {
        try {
            UserDto updatedUser = userProfileClientService.withdrawWalletBalance(parseAmount(withdrawAmountField.getText(), "Withdraw amount"));
            withdrawAmountField.clear();
            refreshProfile(updatedUser);
            showMessage("Wallet updated successfully.", true);
        }
        catch (IOException e) {
            showMessage("Cannot connect to server.", false);
        }
        catch (ValidationException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void handleChangePassword() {
        try {
            userProfileClientService.changePasswordPreview(
                currentPasswordField.getText(),
                newPasswordField.getText(),
                confirmPasswordField.getText()
            );
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            showMessage("Password change passed client validation. Add server-side password update handling to make it real.", true);
        }
        catch (ValidationException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void handleProfileImagePlaceholder() {
        showMessage("Profile image upload is a UI placeholder right now. Add image storage and profile update support on the server when ready.", true);
    }

    private void populateProfile() {
        try {
            refreshProfile(userProfileClientService.getCurrentProfile());
            showMessage("Profile loaded successfully.", true);
        } catch (IOException e) {
            refreshProfile(userProfileClientService.getCachedProfile());
            showMessage("Cannot connect to server.", false);
        } catch (ValidationException e) {
            refreshProfile(userProfileClientService.getCachedProfile());
            showMessage(e.getMessage(), false);
        }

        profileImageHintLabel.setText("Profile image upload placeholder");
    }

    private void refreshProfile(UserDto user) {
        usernameValueLabel.setText(DisplayUtil.defaultText(user.getUsername(), "Unknown"));
        nicknameField.setText(DisplayUtil.defaultText(user.getNickname(), user.getUsername()));
        walletBalanceLabel.setText(DisplayUtil.formatCurrency(user.getWallet()));
        memberStatusLabel.setText("Active bidder");
        String avatarLetter = resolveAvatarLetter(user.getNickname(), user.getUsername());
        profileAvatarLabel.setText(avatarLetter);
        if (missionBarController != null) {
            missionBarController.setAvatarText(avatarLetter);
            missionBarController.setActiveNavigation(null);
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
        missionBarController = SceneManager.getMissionBarController();
        if (missionBarController == null) {
            throw new IllegalStateException("Mission bar was not loaded.");
        }

        auctionsButton = missionBarController.getAuctionsButton();
        createAuctionButton = missionBarController.getCreateAuctionButton();
        missionBarController.setShowExplore(false);
        missionBarController.setShowSearch(false);
        missionBarController.setUseInlineLogout(true);
        missionBarController.setSelectionHandler(this::handleSelection);
        missionBarController.setLogoutHandler(event -> handleLogout());
        missionBarController.setAvatarHandler(event -> SceneManager.switchScene("user-profile.fxml", false, true));
        missionBarController.setActiveNavigation(null);
    }

    private void showMessage(String message, boolean success) {
        if (messageLabel == null) {
            return;
        }
        messageLabel.setText(message == null ? "" : message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        if (message != null && !message.isBlank()) {
            messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
        }
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
}

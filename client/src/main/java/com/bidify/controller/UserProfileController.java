package com.bidify.controller;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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

    @FXML
    private VBox requestsContainer;

    private final UserProfileClientService userProfileClientService = new UserProfileClientService();

    // Store listener references so unsubscribe can remove the exact same object.
    private final Consumer<Event> onWalletChanged = e -> Platform.runLater(() -> refreshProfileFromEvent(e));
    private final Consumer<Event> onLockedBalanceChanged = e -> Platform.runLater(() -> refreshProfileFromEvent(e));
    private final Consumer<Event> onServerNotice = e -> Platform.runLater(() -> NotificationUtil.info(e.getMessage()));
    private final Consumer<Event> onWalletRequestsChanged = e -> Platform.runLater(this::loadRequests);

    // dùng để hiển thị profile hiện tại và đăng ký lắng nghe sự kiện từ server
    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            // dùng để liên kết dữ liệu top bar
            bindTopBar();
            // dùng để đổ dữ liệu vào thông tin tài khoản
            populateProfile();
        });

        EventManager.getInstance().subscribe(EventType.WALLET_CHANGED, onWalletChanged);
        EventManager.getInstance().subscribe(EventType.LOCKED_BALANCE_CHANGED, onLockedBalanceChanged);
        EventManager.getInstance().subscribe(EventType.SERVER_NOTICE, onServerNotice);
        EventManager.getInstance().subscribe(EventType.WALLET_REQUESTS_CHANGED, onWalletRequestsChanged);
    }

    // dùng để hủy lắng nghe sự kiện tránh bị rò rỉ bộ nhớ (memory leak)
    public void cleanup() {
        EventManager.getInstance().unsubscribe(EventType.WALLET_CHANGED, onWalletChanged);
        EventManager.getInstance().unsubscribe(EventType.LOCKED_BALANCE_CHANGED, onLockedBalanceChanged);
        EventManager.getInstance().unsubscribe(EventType.SERVER_NOTICE, onServerNotice);
        EventManager.getInstance().unsubscribe(EventType.WALLET_REQUESTS_CHANGED, onWalletRequestsChanged);
    }

    // dùng để gửi yêu cầu lưu thay đổi nickname của user lên server
    @FXML
    private void handleSaveProfile() {
        try {
            UserDto updatedUser = userProfileClientService.updateProfile(nicknameField.getText());
            // dùng để refresh thông tin tài khoản
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

    // dùng để nạp tiền (gửi request chờ admin duyệt)
    @FXML
    private void handleTopUp() {
        try {
            userProfileClientService.addWalletBalance(parseAmount(topUpAmountField.getText(), "Deposit amount"));
            topUpAmountField.clear();
            NotificationUtil.success("Deposit request submitted — pending admin approval.");
            // dùng để tải danh sách yêu cầu
            loadRequests();
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    // dùng để rút tiền (gửi request khóa số dư chờ admin duyệt)
    @FXML
    private void handleWithdraw() {
        try {
            userProfileClientService.withdrawWalletBalance(parseAmount(withdrawAmountField.getText(), "Withdraw amount"));
            withdrawAmountField.clear();
            NotificationUtil.success("Withdraw request submitted — pending admin approval.");
            // Reload profile since locked balance changed
            // dùng để đổ dữ liệu vào thông tin tài khoản
            populateProfile();
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    // dùng để thay đổi mật khẩu tài khoản
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

    // dùng để xử lý thông tin tài khoản hình ảnh placeholder
    @FXML
    private void handleProfileImagePlaceholder() {
        NotificationUtil.info("Profile image upload is a UI placeholder right now.");
    }

    // dùng để load thông tin người dùng từ server hoặc cache
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
        // dùng để tải danh sách yêu cầu
        loadRequests();
    }

    // dùng để lấy danh sách lịch sử nạp/rút từ server
    private void loadRequests() {
        if (requestsContainer == null) return;
        try {
            List<WalletRequestDto> requests = userProfileClientService.getUserWalletRequests();
            // dùng để hiển thị danh sách yêu cầu
            renderRequests(requests);
        } catch (Exception e) {
            requestsContainer.getChildren().clear();
            requestsContainer.getChildren().add(new Label("Failed to load requests."));
        }
    }

    // dùng để vẽ danh sách các yêu cầu nạp/rút tiền lên UI
    private void renderRequests(List<WalletRequestDto> requests) {
        requestsContainer.getChildren().clear();
        if (requests == null || requests.isEmpty()) {
            Label empty = new Label("No request history.");
            empty.getStyleClass().add("section-copy");
            requestsContainer.getChildren().add(empty);
            return;
        }

        for (WalletRequestDto req : requests) {
            HBox row = new HBox(12);
            row.getStyleClass().add("wallet-req-row");
            row.setAlignment(Pos.CENTER_LEFT);

            VBox details = new VBox(3);
            HBox.setHgrow(details, Priority.ALWAYS);

            String reqTypeStr = req.getType() == TransactionType.DEPOSIT ? "Deposit" : "Withdraw";
            Label title = new Label(reqTypeStr + "  •  $" + String.format("%.2f", req.getAmount()));
            title.getStyleClass().add("wallet-req-title");

            String created = req.getCreatedAt().length() >= 16
                ? req.getCreatedAt().substring(0, 16).replace('T', ' ')
                : req.getCreatedAt();
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
            requestsContainer.getChildren().add(row);
        }
    }

    // dùng để tự động reload lại số dư khi nhận được event đổi ví từ server
    private void refreshProfileFromEvent(Event event) {
        if (event != null && event.getData() != null) {
            UserDto updatedUser = JsonUtil.fromMap(event.getData(), UserDto.class);
            if (updatedUser != null) {
                // dùng để refresh thông tin tài khoản
                refreshProfile(updatedUser);
                return;
            }
        }
        // dùng để đổ dữ liệu vào thông tin tài khoản
        populateProfile();
    }

    // dùng để refresh thông tin tài khoản
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

    // dùng để phân tích cú pháp số tiền
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

    // dùng để liên kết dữ liệu top bar
    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.PROFILE, false, null, this::cleanup);
    }

    // dùng để giải quyết ảnh đại diện letter
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

    // dùng để bật tắt ví controls
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

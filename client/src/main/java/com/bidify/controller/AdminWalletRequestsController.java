package com.bidify.controller;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import com.bidify.common.dto.WalletRequestDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Event;
import com.bidify.event.EventManager;
import com.bidify.model.ClientSession;
import com.bidify.service.AdminClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AdminWalletRequestsController {
    private final AdminClientService adminClientService = new AdminClientService();

    // Stored reference so unsubscribe removes the exact same listener.
    private final Consumer<Event> onWalletRequestsChanged =
        event -> Platform.runLater(this::loadRequests);

    @FXML
    private VBox requestsContainer;

    @FXML
    private Label summaryLabel;

    // dùng để khởi tạo trang quản lý request ví và lắng nghe sự thay đổi từ server
    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            if (!ClientSession.getInstance().isAdmin()) {
                NotificationUtil.error("Only admins can access this page.");
                SceneManager.switchScene("hub.fxml", false, true);
                return;
            }

            MissionBarUtil.setup(NavPage.ADMIN_WALLET_REQUESTS, false, null, this::cleanup);
            EventManager.getInstance().subscribe(EventType.WALLET_REQUESTS_CHANGED, onWalletRequestsChanged);
            // dùng để tải danh sách yêu cầu
            loadRequests();
        });
    }

    // dùng để huỷ đăng ký event tránh leak bộ nhớ
    private void cleanup() {
        EventManager.getInstance().unsubscribe(EventType.WALLET_REQUESTS_CHANGED, onWalletRequestsChanged);
    }

    // dùng để load lại danh sách thủ công
    @FXML
    private void handleRefresh() {
        // dùng để tải danh sách yêu cầu
        loadRequests();
    }

    // dùng để gửi request lấy các yêu cầu nạp/rút chưa duyệt từ server
    private void loadRequests() {
        Platform.runLater(() -> {
            try {
                renderRequests(adminClientService.getPendingWalletRequests());
            }
            catch (IOException e) {
                requestsContainer.getChildren().clear();
                summaryLabel.setText("Cannot connect to server.");
                NotificationUtil.error("Cannot connect to server.");
            }
            catch (ValidationException e) {
                requestsContainer.getChildren().clear();
                summaryLabel.setText(e.getMessage());
                NotificationUtil.error(e.getMessage());
            }
        });
    }

    // dùng để vẽ danh sách các yêu cầu nạp/rút lên bảng điều khiển
    private void renderRequests(List<WalletRequestDto> requests) {
        requestsContainer.getChildren().clear();
        if (requests == null || requests.isEmpty()) {
            summaryLabel.setText("No pending requests");
            requestsContainer.getChildren().add(createEmptyState());
            return;
        }

        summaryLabel.setText("Showing " + requests.size() + " pending request" + (requests.size() == 1 ? "" : "s"));
        for (WalletRequestDto req : requests)
            requestsContainer.getChildren().add(createRequestRow(req));
    }

    // dùng để tạo giao diện hiển thị khi không có yêu cầu nào đang chờ
    private VBox createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().add("admin-empty-state");
        Label title = new Label("No pending requests.");
        title.getStyleClass().add("admin-empty-title");
        Label subtitle = new Label("All wallet requests have been reviewed.");
        subtitle.getStyleClass().add("admin-empty-copy");
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    // dùng để tạo một hàng hiển thị thông tin nạp/rút kèm nút duyệt/từ chối
    private HBox createRequestRow(WalletRequestDto req) {
        HBox row = new HBox(12);
        row.getStyleClass().add("admin-user-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18));

        VBox details = new VBox(4);
        HBox.setHgrow(details, Priority.ALWAYS);

        Label username = new Label(req.getUsername());
        username.getStyleClass().add("admin-user-title");

        String reqTypeStr = req.getType() == TransactionType.DEPOSIT ? "Deposit" : "Withdraw";
        Label subtitle = new Label(reqTypeStr + " • $" + String.format("%.2f", req.getAmount()) + " • " + req.getCreatedAt().substring(0, 16).replace('T', ' '));
        subtitle.getStyleClass().add("admin-user-copy");

        details.getChildren().addAll(username, subtitle);

        Button approveButton = new Button("Approve");
        approveButton.getStyleClass().addAll("admin-action-button");
        approveButton.setOnAction(event -> handleReview(req.getId(), true));

        Button denyButton = new Button("Deny");
        denyButton.getStyleClass().addAll("admin-action-button", "admin-danger-button");
        denyButton.setOnAction(event -> handleReview(req.getId(), false));

        row.getChildren().addAll(details, approveButton, denyButton);
        return row;
    }

    // dùng để gửi quyết định duyệt (Approve) hoặc từ chối (Deny) lên server
    private void handleReview(String requestId, boolean approved) {
        try {
            adminClientService.reviewWalletRequest(requestId, approved);
            NotificationUtil.success("Request " + (approved ? "approved" : "denied") + " successfully.");
            // Wait for event or just reload
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
}

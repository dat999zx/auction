package com.bidify.controller;

import java.io.IOException;
import java.util.List;

import com.bidify.common.dto.AdminUserDto;
import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;
import com.bidify.common.exception.ValidationException;
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

public class AdminUsersController {
    private static final String BOOTSTRAP_ADMIN_USERNAME = "admin";
    private final AdminClientService adminClientService = new AdminClientService();

    @FXML
    private VBox usersContainer;

    @FXML
    private Label summaryLabel;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            if (!ClientSession.getInstance().isAdmin()) {
                NotificationUtil.error("Only admins can access this page.");
                SceneManager.switchScene("hub.fxml", false, true);
                return;
            }

            MissionBarUtil.setup(NavPage.ADMIN_USERS, false, null);
            loadUsers();
        });
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    private void loadUsers() {
        try {
            renderUsers(adminClientService.getUsers());
        }
        catch (IOException e) {
            usersContainer.getChildren().clear();
            summaryLabel.setText("Cannot connect to server.");
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            usersContainer.getChildren().clear();
            summaryLabel.setText(e.getMessage());
            NotificationUtil.error(e.getMessage());
        }
    }

    private void renderUsers(List<AdminUserDto> users) {
        usersContainer.getChildren().clear();
        if (users == null || users.isEmpty()) {
            summaryLabel.setText("No users found");
            usersContainer.getChildren().add(createEmptyState());
            return;
        }

        summaryLabel.setText("Showing " + users.size() + " user" + (users.size() == 1 ? "" : "s"));
        for (AdminUserDto user : users)
            usersContainer.getChildren().add(createUserRow(user));
    }

    private VBox createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().add("admin-empty-state");
        Label title = new Label("No users available.");
        title.getStyleClass().add("admin-empty-title");
        Label subtitle = new Label("Try refreshing again after the server loads user data.");
        subtitle.getStyleClass().add("admin-empty-copy");
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private HBox createUserRow(AdminUserDto user) {
        HBox row = new HBox(12);
        row.getStyleClass().add("admin-user-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(18));

        VBox details = new VBox(4);
        HBox.setHgrow(details, Priority.ALWAYS);

        Label username = new Label(user.getUsername());
        username.getStyleClass().add("admin-user-title");

        Label subtitle = new Label(buildSubtitle(user));
        subtitle.getStyleClass().add("admin-user-copy");

        details.getChildren().addAll(username, subtitle);

        Label status = new Label(user.getStatus().name());
        status.getStyleClass().addAll(
            "admin-status-pill",
            user.getStatus() == UserStatus.BANNED ? "admin-status-banned" : "admin-status-active"
        );

        Button inventoryButton = new Button("Inventory");
        inventoryButton.getStyleClass().add("admin-action-button");
        inventoryButton.setOnAction(event -> openInventory(user.getUsername()));

        Button roleButton = createRoleButton(user);
        Button toggleBanButton = new Button(user.getStatus() == UserStatus.BANNED ? "Unban" : "Ban");
        toggleBanButton.getStyleClass().add("admin-action-button");
        toggleBanButton.setOnAction(event -> handleBanToggle(user));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("admin-action-button", "admin-danger-button");
        deleteButton.setOnAction(event -> handleDelete(user.getUsername()));

        row.getChildren().addAll(details, status, inventoryButton);
        if (roleButton != null)
            row.getChildren().add(roleButton);
        row.getChildren().addAll(toggleBanButton, deleteButton);
        return row;
    }

    private String buildSubtitle(AdminUserDto user) {
        String nickname = user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
        return nickname + " • " + user.getRole().name();
    }

    private void handleBanToggle(AdminUserDto user) {
        try {
            if (user.getStatus() == UserStatus.BANNED) {
                adminClientService.unbanUser(user.getUsername());
                NotificationUtil.success("User unbanned successfully.");
            }
            else {
                adminClientService.banUser(user.getUsername());
                NotificationUtil.success("User banned successfully.");
            }
            loadUsers();
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    private Button createRoleButton(AdminUserDto user) {
        if (!isBootstrapAdminSession())
            return null;
        if (BOOTSTRAP_ADMIN_USERNAME.equals(user.getUsername()))
            return null;

        Button roleButton = new Button(user.getRole() == UserRole.ADMIN ? "Remove Admin" : "Make Admin");
        roleButton.getStyleClass().add("admin-action-button");
        roleButton.setOnAction(event -> handleRoleToggle(user));
        return roleButton;
    }

    private void handleRoleToggle(AdminUserDto user) {
        try {
            if (user.getRole() == UserRole.ADMIN) {
                adminClientService.demoteAdmin(user.getUsername());
                NotificationUtil.success("Admin removed successfully.");
            }
            else {
                adminClientService.promoteAdmin(user.getUsername());
                NotificationUtil.success("User promoted to admin successfully.");
            }
            loadUsers();
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    private boolean isBootstrapAdminSession() {
        return BOOTSTRAP_ADMIN_USERNAME.equals(ClientSession.getInstance().getCurrentUsername());
    }

    private void handleDelete(String username) {
        try {
            adminClientService.deleteUser(username);
            NotificationUtil.success("User deleted successfully.");
            loadUsers();
        }
        catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (ValidationException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    private void openInventory(String username) {
        InventoryController.setManagedOwnerUsername(username);
        SceneManager.clearCache("inventory.fxml");
        SceneManager.switchScene("inventory.fxml", false, true);
    }
}

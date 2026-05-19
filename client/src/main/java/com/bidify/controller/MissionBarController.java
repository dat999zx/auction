package com.bidify.controller;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

public class MissionBarController {
    private static final Duration ANIMATION_DURATION = Duration.millis(180);
    private Button leftSideActiveButton; 

    @FXML
    private HBox searchContainer;

    @FXML
    private Button exploreButton;

    @FXML
    private TextField searchBar;

    @FXML
    private Button auctionsButton; // This is the 'Home' button in sidebar

    @FXML
    private Button createAuctionButton; // This is the 'Create Auctions' button in sidebar sub-menu

    @FXML
    private Button logoutButton;

    @FXML
    private Button logoutLinkButton;

    @FXML
    private Button historyButton;

    @FXML
    private Button inventoryButton;

    @FXML
    private Button adminUsersButton;

    @FXML
    private Button adminWalletRequestsButton;

    @FXML
    private Label avatarText;

    @FXML
    private StackPane avatarContainer;

    @FXML
    private AnchorPane sidebarLayer;

    @FXML
    private Region sidebarOverlay;

    @FXML
    private VBox sidebarContent;

    @FXML
    private Button myAccountButton;

    @FXML
    private Button myActivitiesButton;

    @FXML
    private VBox accountSubMenu;

    @FXML
    private VBox myActivitiesSubMenu;

    private boolean sidebarVisible = false;
    private boolean sidebarAnimating = false;

    // dùng để khởi tạo
    @FXML
    private void initialize() {
        leftSideActiveButton = auctionsButton; // Máº·c Ä‘á»‹nh active home
        double hiddenOffset = getSidebarWidth();
        sidebarLayer.setVisible(false);
        sidebarLayer.setManaged(false);
        sidebarLayer.setMouseTransparent(true);
        sidebarOverlay.setOpacity(0.0);
        sidebarContent.setTranslateX(-hiddenOffset);
    }
    
    // dùng để xử lý ảnh đại diện click
    @FXML
    private void handleAvatarClick(MouseEvent event) {
    }

    // dùng để bật tắt thanh bên điều hướng
    public void toggleSidebar() {
        if (sidebarAnimating) { return; }
        if (sidebarVisible) {
            // dùng để ẩn thanh bên điều hướng
            hideSidebar();
            return;
        }
        // dùng để hiển thị thanh bên điều hướng
        showSidebar();
    }

    // dùng để đóng thanh bên điều hướng
    public void closeSidebar() {
        if (!sidebarVisible || sidebarAnimating) { return; }
        // dùng để ẩn thanh bên điều hướng
        hideSidebar();
    }

    // dùng để xử lý lớp phủ click
    @FXML
    private void handleOverlayClick() {
        // dùng để đóng thanh bên điều hướng
        closeSidebar();
    }

    // dùng để xử lý thanh bên điều hướng click
    @FXML
    private void handleSidebarClick(ActionEvent event) {
        if (!(event.getSource() instanceof Button clickedButton)) return;
        // dùng để cập nhật thanh bên điều hướng nút nhấn style
        updateSidebarButtonStyle(clickedButton);
    }

    // dùng để xử lý account bật tắt
    @FXML
    private void handleAccountToggle(ActionEvent event) {
        boolean isVisible = accountSubMenu.isVisible();
        accountSubMenu.setVisible(!isVisible);
        accountSubMenu.setManaged(!isVisible);
        // dùng để cập nhật thanh bên điều hướng nút nhấn style
        updateSidebarButtonStyle(myAccountButton);
    }

    // dùng để xử lý activities bật tắt
    @FXML
    private void handleActivitiesToggle(ActionEvent event) {
        boolean isVisible = myActivitiesSubMenu.isVisible();
        myActivitiesSubMenu.setVisible(!isVisible);
        myActivitiesSubMenu.setManaged(!isVisible);
        // dùng để cập nhật thanh bên điều hướng nút nhấn style
        updateSidebarButtonStyle(myActivitiesButton);
    }

    // check xem náº¿u nhÆ° button nÃ o Ä‘Æ°á»£c click thÃ¬ sáº½ Ä‘á»•i style cá»§a button Ä‘Ã³ thÃ nh active, cÃ²n láº¡i sáº½ lÃ  normal
    // dùng để cập nhật thanh bên điều hướng nút nhấn style
    private void updateSidebarButtonStyle(Button activeButton) {
        if (leftSideActiveButton == activeButton) return;
        leftSideActiveButton.getStyleClass().removeAll("side-nav-button", "side-nav-button-active");
        leftSideActiveButton.getStyleClass().add("side-nav-button");
        leftSideActiveButton = activeButton;
        leftSideActiveButton.getStyleClass().add("side-nav-button-active");
    }
    

    // dùng để hiển thị thanh bên điều hướng
    private void showSidebar() {
        sidebarAnimating = true;
        double hiddenOffset = getSidebarWidth();
        sidebarLayer.setVisible(true);
        sidebarLayer.setManaged(true);
        sidebarLayer.setMouseTransparent(false);

        FadeTransition overlayFade = new FadeTransition(ANIMATION_DURATION, sidebarOverlay);
        overlayFade.setFromValue(0.0);
        overlayFade.setToValue(1.0);

        TranslateTransition sidebarSlide = new TranslateTransition(ANIMATION_DURATION, sidebarContent);
        sidebarSlide.setFromX(-hiddenOffset);
        sidebarSlide.setToX(0.0);

        sidebarSlide.setOnFinished(event -> {
            sidebarVisible = true;
            sidebarAnimating = false;
        });

        overlayFade.play();
        sidebarSlide.play();
    }

    // thÃªm cÃ¡c getter Ä‘á»ƒ sá»­ dá»¥ng cho cÃ¡c file fxml cÃ³ missionbar controller
    // dùng để lấy khám phá nút nhấn
    public Button getExploreButton() { return exploreButton; }
    // dùng để lấy tìm kiếm bar
    public TextField getSearchBar() { return searchBar; }
    // dùng để lấy danh sách đấu giá nút nhấn
    public Button getAuctionsButton() { return auctionsButton; }
    // dùng để lấy tạo đấu giá nút nhấn
    public Button getCreateAuctionButton() { return createAuctionButton; }
    // dùng để lấy đăng xuất nút nhấn
    public Button getLogoutButton() { return logoutButton; }
    // dùng để lấy đăng xuất link nút nhấn
    public Button getLogoutLinkButton() { return logoutLinkButton; }
    // dùng để lấy lịch sử nút nhấn
    public Button getHistoryButton() { return historyButton; }
    // dùng để lấy kho đồ nút nhấn
    public Button getInventoryButton() { return inventoryButton; }
    // dùng để lấy quản trị viên (admin) danh sách người dùng nút nhấn
    public Button getAdminUsersButton() { return adminUsersButton; }
    // dùng để lấy nút xem các yêu cầu ví trong thanh quản trị
    public Button getAdminWalletRequestsButton() { return adminWalletRequestsButton; }

    // dùng để thiết lập hiển thị khám phá
    public void setShowExplore(boolean visible) {
        exploreButton.setManaged(visible);
        exploreButton.setVisible(visible);
    }

    // dùng để thiết lập hiển thị tìm kiếm
    public void setShowSearch(boolean visible) {
        searchContainer.setManaged(visible);
        searchContainer.setVisible(visible);
    }

    // dùng để thiết lập use inline đăng xuất
    public void setUseInlineLogout(boolean useInlineLogout) {
        logoutButton.setManaged(useInlineLogout);
        logoutButton.setVisible(useInlineLogout);
        logoutLinkButton.setManaged(!useInlineLogout);
        logoutLinkButton.setVisible(!useInlineLogout);
    }

    // dùng để thiết lập hiển thị quản trị viên (admin) controls
    public void setShowAdminControls(boolean visible) {
        if (adminUsersButton != null) {
            adminUsersButton.setManaged(visible);
            adminUsersButton.setVisible(visible);
        }
        if (adminWalletRequestsButton != null) {
            adminWalletRequestsButton.setManaged(visible);
            adminWalletRequestsButton.setVisible(visible);
        }
    }

    // dùng để thiết lập hiển thị tạo đấu giá
    public void setShowCreateAuction(boolean visible) {
        if (createAuctionButton == null) return;
        createAuctionButton.setManaged(visible);
        createAuctionButton.setVisible(visible);
    }

    // dùng để thiết lập selection trình xử lý
    public void setSelectionHandler(EventHandler<ActionEvent> handler) {
        auctionsButton.setOnAction(handler);
        createAuctionButton.setOnAction(handler);
        logoutLinkButton.setOnAction(handler);
        if (historyButton != null) {
            historyButton.setOnAction(handler);
        }
        if (inventoryButton != null) {
            inventoryButton.setOnAction(handler);
        }
        if (adminUsersButton != null) {
            adminUsersButton.setOnAction(handler);
        }
        if (adminWalletRequestsButton != null) {
            adminWalletRequestsButton.setOnAction(handler);
        }
    }

    // dùng để thiết lập ảnh đại diện trình xử lý
    public void setAvatarHandler(EventHandler<MouseEvent> handler) {
        avatarContainer.setOnMouseClicked(handler);
    }

    // dùng để thiết lập khám phá trình xử lý
    public void setExploreHandler(EventHandler<ActionEvent> handler) {
        exploreButton.setOnAction(handler);
    }

    // dùng để thiết lập đăng xuất trình xử lý
    public void setLogoutHandler(EventHandler<ActionEvent> handler) {
        logoutButton.setOnAction(handler);
    }

    // dùng để thiết lập ảnh đại diện text
    public void setAvatarText(String value) {
        avatarText.setText(value == null || value.isBlank() ? "U" : value);
    }

    // dùng để thiết lập active navigation
    public void setActiveNavigation(Button activeButton) {
        if (activeButton == auctionsButton || activeButton == createAuctionButton || activeButton == historyButton || activeButton == inventoryButton || activeButton == adminUsersButton || activeButton == adminWalletRequestsButton) {
            // dùng để cập nhật thanh bên điều hướng nút nhấn style
            updateSidebarButtonStyle(activeButton);
        }
        
        // dùng để cập nhật nav nút nhấn style
        updateNavButtonStyle(logoutLinkButton, activeButton == logoutLinkButton);
    }

    // dùng để ẩn thanh bên điều hướng
    private void hideSidebar() {
        sidebarAnimating = true;
        double hiddenOffset = getSidebarWidth();

        FadeTransition overlayFade = new FadeTransition(ANIMATION_DURATION, sidebarOverlay);
        overlayFade.setFromValue(sidebarOverlay.getOpacity());
        overlayFade.setToValue(0.0);

        TranslateTransition sidebarSlide = new TranslateTransition(ANIMATION_DURATION, sidebarContent);
        sidebarSlide.setFromX(sidebarContent.getTranslateX());
        sidebarSlide.setToX(-hiddenOffset);

        sidebarSlide.setOnFinished(event -> {
            sidebarVisible = false;
            sidebarAnimating = false;
            sidebarLayer.setVisible(false);
            sidebarLayer.setManaged(false);
            sidebarLayer.setMouseTransparent(true);
        });

        overlayFade.play();
        sidebarSlide.play();
    }

    // dùng để cập nhật nav nút nhấn style
    private void updateNavButtonStyle(Button button, boolean active) {
        if (button == null) return;

        button.getStyleClass().removeAll("top-link", "top-link-active");
        button.getStyleClass().add(active ? "top-link-active" : "top-link");
    }

    // dùng để lấy thanh bên điều hướng width
    private double getSidebarWidth() {
        if (sidebarContent == null) {
            return 340.0;
        }

        double width = sidebarContent.getPrefWidth();
        if (width <= 0 && sidebarContent.getWidth() > 0) {
            width = sidebarContent.getWidth();
        }

        return width > 0 ? width : 340.0;
    }
}

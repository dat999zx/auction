package com.bidify.controller;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
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
    private Button settlementsButton;

    @FXML
    private Button adminUsersButton;

    @FXML
    private Button adminWalletRequestsButton;

    @FXML
    private Button adminAuctionsButton;

    @FXML
    private Label avatarText;
    
    @FXML
    private ImageView avatarImageView;

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

    @FXML 
    private Button myAuctionsButton;

    private boolean sidebarVisible = false;
    private boolean sidebarAnimating = false;

    // dùng để khởi tạo
    @FXML
    private void initialize() {
        leftSideActiveButton = auctionsButton; // Mặc định active home
        double hiddenOffset = getSidebarWidth();
        sidebarLayer.setVisible(false);
        sidebarLayer.setManaged(false);
        sidebarLayer.setMouseTransparent(true);
        sidebarOverlay.setOpacity(0.0);
        sidebarContent.setTranslateX(-hiddenOffset);

        // dùng để cắt ảnh đại diện thành hình tròn
        Circle clip = new Circle(20, 20, 20);
        avatarImageView.setClip(clip);
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

    // check xem nếu như button nào được click thì sẽ đổi style của button đó thành active, còn lại sẽ là normal
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

    public Button getExploreButton() { return exploreButton; }
    public TextField getSearchBar() { return searchBar; }
    public Button getAuctionsButton() { return auctionsButton; }
    public Button getCreateAuctionButton() { return createAuctionButton; }
    public Button getLogoutButton() { return logoutButton; }
    public Button getLogoutLinkButton() { return logoutLinkButton; }
    public Button getHistoryButton() { return historyButton; }
    public Button getInventoryButton() { return inventoryButton; }
    public Button getSettlementsButton() { return settlementsButton; }
    public Button getAdminUsersButton() { return adminUsersButton; }
    public Button getAdminWalletRequestsButton() { return adminWalletRequestsButton; }
    public Button getAdminAuctionsButton() { return adminAuctionsButton; }
    public Button getMyAuctionsButton() { return myAuctionsButton; }
    
    // dùng để thiết lập hiển thị khám phá
    public void setShowExplore(boolean visible) {
        exploreButton.setManaged(visible);
        exploreButton.setVisible(visible);
    }

    public void setShowSearch(boolean visible) {
        searchContainer.setManaged(visible);
        searchContainer.setVisible(visible);
    }

    public void setUseInlineLogout(boolean useInlineLogout) {
        logoutButton.setManaged(useInlineLogout);
        logoutButton.setVisible(useInlineLogout);
        logoutLinkButton.setManaged(!useInlineLogout);
        logoutLinkButton.setVisible(!useInlineLogout);
    }

    public void setShowAdminControls(boolean visible) {
        if (adminUsersButton != null) {
            adminUsersButton.setManaged(visible);
            adminUsersButton.setVisible(visible);
        }
        if (adminWalletRequestsButton != null) {
            adminWalletRequestsButton.setManaged(visible);
            adminWalletRequestsButton.setVisible(visible);
        }
        if (adminAuctionsButton != null) {
            adminAuctionsButton.setManaged(visible);
            adminAuctionsButton.setVisible(visible);
        }
    }

    public void setShowCreateAuction(boolean visible) {
        if (createAuctionButton == null) return;
        createAuctionButton.setManaged(visible);
        createAuctionButton.setVisible(visible);
    }

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
        if (settlementsButton != null) {
            settlementsButton.setOnAction(handler);
        }
        if (adminUsersButton != null) {
            adminUsersButton.setOnAction(handler);
        }
        if (adminWalletRequestsButton != null) {
            adminWalletRequestsButton.setOnAction(handler);
        }
        if (adminAuctionsButton != null) {
            adminAuctionsButton.setOnAction(handler);
        }
        if (myAuctionsButton != null) {
            myAuctionsButton.setOnAction(handler);
        }
    }

    public void setAvatarHandler(EventHandler<MouseEvent> handler) {
        avatarContainer.setOnMouseClicked(handler);
    }

    public void setExploreHandler(EventHandler<ActionEvent> handler) {
        exploreButton.setOnAction(handler);
    }

    public void setLogoutHandler(EventHandler<ActionEvent> handler) {
        logoutButton.setOnAction(handler);
    }

    public void setAvatarText(String value) {
        avatarText.setText(value == null || value.isBlank() ? "U" : value);
    }

    public void setAvatarImage(Image image) {
        boolean hasImage = image != null;
        avatarImageView.setImage(image);
        avatarImageView.setVisible(hasImage);
        avatarText.setVisible(!hasImage);
    }

    public void setActiveNavigation(Button activeButton) {
        if (activeButton != null) {
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

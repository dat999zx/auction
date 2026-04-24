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

    @FXML
    private HBox searchContainer;

    @FXML
    private Button exploreButton;

    @FXML
    private TextField searchBar;

    @FXML
    private Button auctionsButton;

    @FXML
    private Button createAuctionButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button logoutLinkButton;

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

    private boolean sidebarVisible = false;
    private boolean sidebarAnimating = false;

    @FXML
    private void initialize() {
        double hiddenOffset = getSidebarWidth();
        sidebarLayer.setVisible(false);
        sidebarLayer.setManaged(false);
        sidebarLayer.setMouseTransparent(true);
        sidebarOverlay.setOpacity(0.0);
        sidebarContent.setTranslateX(-hiddenOffset);
    }

    // thêm các getter để sử dụng cho các file fxml có missionbar controller
    public Button getExploreButton() { return exploreButton; }
    public TextField getSearchBar() { return searchBar; }
    public Button getAuctionsButton() { return auctionsButton; }
    public Button getCreateAuctionButton() { return createAuctionButton; }
    public Button getLogoutButton() { return logoutButton; }
    public Button getLogoutLinkButton() { return logoutLinkButton; }

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

    public void setSelectionHandler(EventHandler<ActionEvent> handler) {
        auctionsButton.setOnAction(handler);
        createAuctionButton.setOnAction(handler);
        logoutLinkButton.setOnAction(handler);
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

    public void setActiveNavigation(Button activeButton) {
        updateNavButtonStyle(auctionsButton, activeButton == auctionsButton);
        updateNavButtonStyle(createAuctionButton, activeButton == createAuctionButton);
        updateNavButtonStyle(logoutLinkButton, activeButton == logoutLinkButton);
    }

    @FXML
    private void handleAvatarClick(MouseEvent event) {
    }

    public void toggleSidebar() {
        if (sidebarAnimating) { return; }
        if (sidebarVisible) {
            hideSidebar();
            return;
        }
        showSidebar();
    }

    public void closeSidebar() {
        if (!sidebarVisible || sidebarAnimating) { return; }
        hideSidebar();
    }

    @FXML
    private void handleOverlayClick() {
        closeSidebar();
    }

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

    private void updateNavButtonStyle(Button button, boolean active) {
        if (button == null) {
            return;
        }

        button.getStyleClass().removeAll("top-link", "top-link-active");
        button.getStyleClass().add(active ? "top-link-active" : "top-link");
    }

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

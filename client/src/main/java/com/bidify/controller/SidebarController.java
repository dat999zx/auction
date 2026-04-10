package com.bidify.controller;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SidebarController {
    private static final Duration ANIMATION_DURATION = Duration.millis(180);
    private static final double SIDEBAR_WIDTH = 250.0;

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
        sidebarLayer.setVisible(false);
        sidebarLayer.setManaged(false);
        sidebarLayer.setMouseTransparent(true);
        sidebarOverlay.setOpacity(0.0);
        sidebarContent.setTranslateX(-SIDEBAR_WIDTH);
    }

    public void toggleSidebar() {
        if (sidebarAnimating) {
            return;
        }

        if (sidebarVisible) {
            hideSidebar();
            return;
        }

        showSidebar();
    }

    public void closeSidebar() {
        if (!sidebarVisible || sidebarAnimating) {
            return;
        }

        hideSidebar();
    }

    @FXML
    private void handleOverlayClick() {
        closeSidebar();
    }

    private void showSidebar() {
        sidebarAnimating = true;
        sidebarLayer.setVisible(true);
        sidebarLayer.setManaged(true);
        sidebarLayer.setMouseTransparent(false);

        FadeTransition overlayFade = new FadeTransition(ANIMATION_DURATION, sidebarOverlay);
        overlayFade.setFromValue(0.0);
        overlayFade.setToValue(1.0);

        TranslateTransition sidebarSlide = new TranslateTransition(ANIMATION_DURATION, sidebarContent);
        sidebarSlide.setFromX(-SIDEBAR_WIDTH);
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

        FadeTransition overlayFade = new FadeTransition(ANIMATION_DURATION, sidebarOverlay);
        overlayFade.setFromValue(sidebarOverlay.getOpacity());
        overlayFade.setToValue(0.0);

        TranslateTransition sidebarSlide = new TranslateTransition(ANIMATION_DURATION, sidebarContent);
        sidebarSlide.setFromX(sidebarContent.getTranslateX());
        sidebarSlide.setToX(-SIDEBAR_WIDTH);

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
}

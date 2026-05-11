package com.bidify.controller;

import java.io.IOException;
import com.bidify.network.SocketClient;
import com.bidify.common.enums.RequestStatus;
import com.bidify.service.AuthClientService;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryController {
    private static final Logger logger = LoggerFactory.getLogger(HistoryController.class);
    private MissionBarController missionBarController;
    private final AuthClientService authClientService = new AuthClientService();

    @FXML
    private void initialize() {
        Platform.runLater(this::bindTopBar);
    }

    private void bindTopBar() {
        missionBarController = SceneManager.getMissionBarController();
        if (missionBarController == null) {
            logger.warn("Mission bar was not loaded for HistoryController.");
            return;
        }

        missionBarController.setShowExplore(true);
        missionBarController.setShowSearch(false); 
        missionBarController.setUseInlineLogout(true);
        missionBarController.setSelectionHandler(this::handleSelection);
        missionBarController.setExploreHandler(event -> missionBarController.toggleSidebar());
        missionBarController.setLogoutHandler(event -> handleLogout());
        missionBarController.setAvatarHandler(event -> SceneManager.switchScene("user-profile.fxml", false, true));
        missionBarController.setAvatarText(resolveAvatarLetter());
        missionBarController.setActiveNavigation(null); 
    }

    private void handleSelection(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) return;
        
        if (selectedButton == missionBarController.getAuctionsButton()) {
            SceneManager.switchScene("hub.fxml", true, true);
        } else if (selectedButton == missionBarController.getCreateAuctionButton()) {
            SceneManager.switchScene("create-auction.fxml", false, true);
        }
    }

    private void handleLogout() {
        try {
            var response = authClientService.logout();
            if (response.getStatus() == RequestStatus.SUCCESS) {
                NotificationUtil.success("Logged out successfully.");
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml", true, false);
            }
        } catch (IOException e) {
            logger.error("Logout failed", e);
        }
    }

    private String resolveAvatarLetter() {
        String username = SocketClient.getClient().getCurrentUsername();
        return (username == null || username.isBlank()) ? "U" : username.substring(0, 1).toUpperCase();
    }
}

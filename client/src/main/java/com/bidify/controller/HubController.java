package com.bidify.controller;

import java.io.IOException;

import com.bidify.network.SocketClient;
import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.service.AuctionClientService;
import com.bidify.service.AuthClientService;
import com.bidify.utility.SceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HubController {
    private static final Logger logger = LoggerFactory.getLogger(HubController.class);
    private static final double AUCTION_ROW_GAP = 56.0;

    @FXML
    private TextField searchBar;

    @FXML
    private Button auctionsButton;

    @FXML
    private Button createAuctionButton;

    @FXML
    private VBox liveAuctionsContainer;

    @FXML
    private Label emptyStateLabel;

    private MissionBarController missionBarController;

    private AuctionDto[] currentAuctions = new AuctionDto[0];
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final AuthClientService authClientService = new AuthClientService();

    @FXML
    private void initialize() {
        Platform.runLater(this::bindTopBar);
        loadLiveAuctions();
    }

    @FXML
    private void toggleSidebar() {
        if (missionBarController != null) {
            missionBarController.toggleSidebar();
        }
    }

    @FXML
    private void handleSelection(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) {
            return;
        }

        if (selectedButton == createAuctionButton) {
            handleCreateAuction();
        }

    }

    @FXML
    private void handleLogout() {
        String currentUsername = com.bidify.network.SocketClient.getClient().getCurrentUsername();

        if (currentUsername == null || currentUsername.isBlank()) {
            SceneManager.clearAllCache();
            SceneManager.switchScene("login.fxml", true, false);
            return;
        }

        try {
            var response = authClientService.logout();
            if (response.getStatus() == RequestStatus.SUCCESS) {
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml", true, false);
                return;
            }
            logger.error("Logout failed: {}", response.getMessage());
        }
        catch (IOException e) {
            logger.error("Exception occurred", e);
        }
    }

    private void loadLiveAuctions() {
        try {
            AuctionDto[] auctions = auctionClientService.getLiveAuctions();
            if (auctions == null || auctions.length == 0) {
                Platform.runLater(() -> showEmptyState("No live auctions right now."));
                return;
            }

            currentAuctions = auctions;
            Platform.runLater(() -> {
                emptyStateLabel.setVisible(false);
                emptyStateLabel.setManaged(false);
                renderAuctionRows();
            });
        } catch (IOException e) {
            logger.error("Exception occurred", e);
            Platform.runLater(() -> showEmptyState("Cannot connect to server."));
        } catch (AuctionException e) {
            Platform.runLater(() -> showEmptyState(e.getMessage()));
        }
    }

    private void showEmptyState(String message) {
        currentAuctions = new AuctionDto[0];
        liveAuctionsContainer.getChildren().clear();
        emptyStateLabel.setText(message);
        if (!emptyStateLabel.getStyleClass().contains("empty-state-label")) {
            emptyStateLabel.getStyleClass().add("empty-state-label");
        }
        emptyStateLabel.setManaged(true);
        emptyStateLabel.setVisible(true);
    }

    private void renderAuctionRows() {
        liveAuctionsContainer.getChildren().clear();
        if (currentAuctions == null || currentAuctions.length == 0) {
            return;
        }

        int cardsPerRow = 2;
        for (int i = 0; i < currentAuctions.length; i += cardsPerRow) {
            HBox row = new HBox(AUCTION_ROW_GAP);
            row.setAlignment(Pos.TOP_CENTER);

            for (int j = 0; j < cardsPerRow && i + j < currentAuctions.length; j++) {
                row.getChildren().add(loadAuctionCard(currentAuctions[i + j]));
            }

            liveAuctionsContainer.getChildren().add(row);
        }
    }

    private AnchorPane loadAuctionCard(AuctionDto auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-card.fxml"));
            AnchorPane card = loader.load();
            AuctionCardController controller = loader.getController();
            controller.bind(auction);
            return card;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load auction-card.fxml", e);
        }
    }

    private void search(){
        if (searchBar.getText() == null || searchBar.getText().isBlank()) return;
        //TODO: search key AuctionName, AuctionId
        

    }

    private void handleCreateAuction() {
        SceneManager.switchScene("create-auction.fxml", false, true);
    }

    private void bindTopBar() {
        missionBarController = SceneManager.getMissionBarController();
        if (missionBarController == null) {
            throw new IllegalStateException("Mission bar was not loaded.");
        }

        searchBar = missionBarController.getSearchBar();
        auctionsButton = missionBarController.getAuctionsButton();
        createAuctionButton = missionBarController.getCreateAuctionButton();
        missionBarController.setShowExplore(true);
        missionBarController.setShowSearch(true);
        missionBarController.setUseInlineLogout(true);
        missionBarController.setSelectionHandler(this::handleSelection);
        missionBarController.setExploreHandler(event -> toggleSidebar());
        missionBarController.setLogoutHandler(event -> handleLogout());
        missionBarController.setAvatarHandler(event -> SceneManager.switchScene("user-profile.fxml", false, true));
        missionBarController.setAvatarText(resolveAvatarLetter());
        missionBarController.setActiveNavigation(auctionsButton);
    }

    private String resolveAvatarLetter() {
        String username = SocketClient.getClient().getCurrentUsername();
        if (username == null || username.isBlank()) {
            return "U";
        }
        return username.substring(0, 1).toUpperCase();
    }

}

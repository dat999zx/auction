package com.bidify.controller;

import java.io.IOException;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.service.AuctionClientService;
import com.bidify.service.AuthClientService;
import com.bidify.utility.SceneManager;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
public class HubController {
    private static final javafx.util.Duration SIDEBAR_ANIMATION_DURATION = javafx.util.Duration.millis(160);
    private static final double SIDEBAR_EXPANDED_WIDTH = 250.0;
    private static final double SIDEBAR_VISIBLE_ROW_GAP = 24.0;
    private static final double SIDEBAR_HIDDEN_ROW_GAP = 56.0;

    @FXML
    private HBox topBar;

    @FXML
    private SharedTopBarController topBarController;

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

    @FXML
    private StackPane sidebarContainer;

    @FXML
    private VBox sidebarContent;

    private boolean sidebarVisible = false;
    private boolean sidebarAnimating = false;
    private AuctionDto[] currentAuctions = new AuctionDto[0];
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final AuthClientService authClientService = new AuthClientService();

    @FXML
    private void initialize() {
        bindTopBar();
        auctionsButton.getStyleClass().removeAll("top-link");
        if (!auctionsButton.getStyleClass().contains("top-link-active")) {
            auctionsButton.getStyleClass().add("top-link-active");
        }

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarContainer.widthProperty());
        clip.heightProperty().bind(sidebarContainer.heightProperty());
        sidebarContainer.setClip(clip);

        initializeSidebarState();
        loadLiveAuctions();
    }

    @FXML
    private void toggleSidebar() {
        if (sidebarAnimating) {
            return;
        }

        sidebarAnimating = true;
        double targetWidth = sidebarVisible ? 0.0 : SIDEBAR_EXPANDED_WIDTH;
        double targetTranslateX = sidebarVisible ? -SIDEBAR_EXPANDED_WIDTH : 0.0;

        TranslateTransition slideTransition = new TranslateTransition(SIDEBAR_ANIMATION_DURATION, sidebarContent);
        slideTransition.setToX(targetTranslateX);

        Timeline resizeTimeline = new Timeline(
            new KeyFrame(
                SIDEBAR_ANIMATION_DURATION,
                new KeyValue(sidebarContainer.prefWidthProperty(), targetWidth),
                new KeyValue(sidebarContainer.minWidthProperty(), targetWidth),
                new KeyValue(sidebarContainer.maxWidthProperty(), targetWidth)
            )
        );

        slideTransition.setOnFinished(event -> {
            sidebarVisible = !sidebarVisible;
            sidebarAnimating = false;
            renderAuctionRows();
        });

        sidebarContent.setMouseTransparent(sidebarVisible);
        slideTransition.play();
        resizeTimeline.play();
    }

    @FXML
    private void handleSelection(ActionEvent event) {
        if (event.getSource() instanceof Button selectedButton) {
            if (selectedButton == createAuctionButton) {
                handleCreateAuction();
            }
                
        }
    }

    @FXML
    private void handleLogout() {
        String currentUsername = com.bidify.network.SocketClient.getClient().getCurrentUsername();

        if (currentUsername == null || currentUsername.isBlank()) {
            SceneManager.clearAllCache();
            SceneManager.switchScene("login.fxml");
            return;
        }

        try {
            var response = authClientService.logout();
            if (response.getStatus() == RequestStatus.SUCCESS) {
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml");
                return;
            }
            System.err.println("Logout failed: " + response.getMessage());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLiveAuctions() {
        try {
            AuctionDto[] auctions = auctionClientService.getLiveAuctions();
            if (auctions == null || auctions.length == 0) {
                showEmptyState("No live auctions right now.");
                return;
            }

            currentAuctions = auctions;
            emptyStateLabel.setVisible(false);
            emptyStateLabel.setManaged(false);
            renderAuctionRows();
        } catch (IOException e) {
            showEmptyState("Cannot connect to server.");
            e.printStackTrace();
        } catch (AuctionException e) {
            showEmptyState(e.getMessage());
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

    private void initializeSidebarState() {
        sidebarContainer.setPrefWidth(0.0);
        sidebarContainer.setMinWidth(0.0);
        sidebarContainer.setMaxWidth(0.0);
        sidebarContent.setTranslateX(-SIDEBAR_EXPANDED_WIDTH);
        sidebarContent.setMouseTransparent(true);
    }

    private void renderAuctionRows() {
        liveAuctionsContainer.getChildren().clear();
        if (currentAuctions == null || currentAuctions.length == 0) {
            return;
        }

        int cardsPerRow = 2;
        double rowGap = sidebarVisible ? SIDEBAR_VISIBLE_ROW_GAP : SIDEBAR_HIDDEN_ROW_GAP;
        for (int i = 0; i < currentAuctions.length; i += cardsPerRow) {
            HBox row = new HBox(rowGap);
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

    // private void setActiveTopNav(Button activeButton) {
    //     Button[] topNavButtons = { auctionsButton, createAuctionButton };

    //     for (Button button : topNavButtons) {
    //         if (button == null) {
    //             continue;
    //         }
    //         button.getStyleClass().removeAll("top-link", "top-link-active");
    //         button.getStyleClass().add(button == activeButton ? "top-link-active" : "top-link");
    //     }
    // }

    private void search(){
        if (searchBar.getText() == null || searchBar.getText().isBlank()) return;
        //TODO: search key AuctionName, AuctionId, Auction Category

    }

    private void handleCreateAuction() {
        SceneManager.switchScene("create-auction.fxml");
    }

    private void bindTopBar() {
        if (topBarController == null) {
            throw new IllegalStateException("Shared top bar was not loaded.");
        }

        searchBar = topBarController.getSearchBar();
        auctionsButton = topBarController.getAuctionsButton();
        createAuctionButton = topBarController.getCreateAuctionButton();

        topBarController.setShowExplore(true);
        topBarController.setShowSearch(true);
        topBarController.setUseInlineLogout(true);
        topBarController.setSelectionHandler(this::handleSelection);
        topBarController.setExploreHandler(event -> toggleSidebar());
        topBarController.setLogoutHandler(event -> handleLogout());
    }

}

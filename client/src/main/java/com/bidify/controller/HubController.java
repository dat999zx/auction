package com.bidify.controller;

import java.io.IOException;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.Event;
import com.bidify.event.EventManager;
import com.bidify.service.AuctionClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.SceneManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HubController {
    private static final Logger logger = LoggerFactory.getLogger(HubController.class);

    @FXML
    private FlowPane liveAuctionsContainer;

    @FXML
    private VBox header;

    @FXML
    private Label pageTitle;

    @FXML
    private Label emptyStateLabel;

    private AuctionDto[] currentAuctions = new AuctionDto[0];
    private final AuctionClientService auctionClientService = new AuctionClientService();

    @FXML
    private void initialize() {
        Platform.runLater(this::bindTopBar);
        loadLiveAuctions();

        EventManager.getInstance().subscribe(EventType.AUCTION_CREATED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_UPDATED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_DELETED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleAuctionEvent);
    }

    private void handleAuctionEvent(Event event) {
        Platform.runLater(this::loadLiveAuctions);
    }

    public void cleanup() {
        EventManager.getInstance().unsubscribe(EventType.AUCTION_CREATED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_UPDATED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_DELETED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_ENDED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.BID_PLACED, this::handleAuctionEvent);
    }

    private void loadLiveAuctions() {
        try {
            AuctionDto[] auctions = auctionClientService.getLiveAuctions();
            
            Platform.runLater(() -> {
                header.setVisible(true);
                header.setManaged(true);
                pageTitle.setText("Live Running Auctions");
            });

            if (auctions == null || auctions.length == 0) {
                Platform.runLater(() -> showEmptyState("No live auctions right now.", false));
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
            Platform.runLater(() -> showEmptyState("Cannot connect to server.", true));
        } catch (AuctionException e) {
            Platform.runLater(() -> showEmptyState(e.getMessage(), true));
        }
    }

    private void showEmptyState(String message, boolean hideHeader) {
        currentAuctions = new AuctionDto[0];
        liveAuctionsContainer.getChildren().clear();
        
        Platform.runLater(() -> {
            header.setVisible(!hideHeader);
            header.setManaged(!hideHeader);
            emptyStateLabel.setText(message);
            if (!emptyStateLabel.getStyleClass().contains("empty-state-label")) {
                emptyStateLabel.getStyleClass().add("empty-state-label");
            }
            emptyStateLabel.setManaged(true);
            emptyStateLabel.setVisible(true);
        });
    }

    private void renderAuctionRows() {
        liveAuctionsContainer.getChildren().clear();
        if (currentAuctions == null || currentAuctions.length == 0) {
            return;
        }

        for (AuctionDto auction : currentAuctions) {
            liveAuctionsContainer.getChildren().add(loadAuctionCard(auction));
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

    private void search() {
        TextField searchBar = SceneManager.getMissionBarController().getSearchBar();
        String query = searchBar.getText();
        if (query == null || query.isBlank()) {
            loadLiveAuctions();
            return;
        }

        try {
            AuctionDto[] results = auctionClientService.searchAuctions(query);
            if (results == null || results.length == 0) {
                showEmptyState("No auctions or sellers found matching '" + query + "'.", true);
                return;
            }

            currentAuctions = results;
            Platform.runLater(() -> {
                header.setVisible(true);
                header.setManaged(true);
                pageTitle.setText("Results for '" + query + "'");
                
                emptyStateLabel.setVisible(false);
                emptyStateLabel.setManaged(false);
                renderAuctionRows();
            });
        } catch (IOException e) {
            logger.error("Search failed", e);
            Platform.runLater(() -> showEmptyState("Search failed: Network error.", true));
        } catch (AuctionException e) {
            Platform.runLater(() -> showEmptyState(e.getMessage(), true));
        }
    }

    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.HOME, true, event -> search(), this::cleanup);
    }
}

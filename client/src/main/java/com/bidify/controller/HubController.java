package com.bidify.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.Event;
import com.bidify.event.EventManager;
import com.bidify.service.AuctionClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class HubController {
    private static final Logger logger = LoggerFactory.getLogger(HubController.class);

    @FXML
    private VBox defaultSections;

    @FXML
    private VBox searchSection;

    @FXML
    private HBox liveAuctionsContainer;

    @FXML
    private HBox upcomingAuctionsContainer;

    @FXML
    private HBox searchResultsContainer;

    @FXML
    private Label liveEmptyStateLabel;

    @FXML
    private Label upcomingEmptyStateLabel;

    @FXML
    private Label searchEmptyStateLabel;

    @FXML
    private Label searchTitleLabel;

    private AuctionDto[] liveAuctions = new AuctionDto[0];
    private AuctionDto[] upcomingAuctions = new AuctionDto[0];
    private AuctionDto[] searchResults = new AuctionDto[0];
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final List<AuctionCardController> renderedCardControllers = new ArrayList<>();

    @FXML
    private void initialize() {
        Platform.runLater(this::bindTopBar);
        loadHubSections();

        EventManager.getInstance().subscribe(EventType.AUCTION_CREATED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_UPDATED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_DELETED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleAuctionEvent);
    }

    private void handleAuctionEvent(Event event) {
        Platform.runLater(this::loadHubSections);
    }

    public void cleanup() {
        cleanupRenderedCards();
        EventManager.getInstance().unsubscribe(EventType.AUCTION_CREATED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_UPDATED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_DELETED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_ENDED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.BID_PLACED, this::handleAuctionEvent);
    }

    private void loadHubSections() {
        try {
            AuctionDto[] live = auctionClientService.getLiveAuctions();
            AuctionDto[] upcoming = auctionClientService.getUpcomingAuctions();

            liveAuctions = live == null ? new AuctionDto[0] : live;
            upcomingAuctions = upcoming == null ? new AuctionDto[0] : upcoming;

            Platform.runLater(() -> {
                cleanupRenderedCards();
                showDefaultSections();
                renderSection(liveAuctionsContainer, liveEmptyStateLabel, liveAuctions,
                        "No live auctions right now.");
                renderSection(upcomingAuctionsContainer, upcomingEmptyStateLabel, upcomingAuctions,
                        "No upcoming auctions right now.");
            });
        } catch (IOException e) {
            logger.error("Failed to load hub auctions", e);
            Platform.runLater(() -> {
                cleanupRenderedCards();
                showDefaultSections();
                renderSection(liveAuctionsContainer, liveEmptyStateLabel, new AuctionDto[0], "Cannot connect to server.");
                renderSection(upcomingAuctionsContainer, upcomingEmptyStateLabel, new AuctionDto[0], "Cannot connect to server.");
            });
        } catch (AuctionException e) {
            Platform.runLater(() -> {
                cleanupRenderedCards();
                showDefaultSections();
                renderSection(liveAuctionsContainer, liveEmptyStateLabel, new AuctionDto[0], e.getMessage());
                renderSection(upcomingAuctionsContainer, upcomingEmptyStateLabel, new AuctionDto[0],
                        "No upcoming auctions right now.");
            });
        }
    }

    private void showDefaultSections() {
        defaultSections.setVisible(true);
        defaultSections.setManaged(true);
        searchSection.setVisible(false);
        searchSection.setManaged(false);
    }

    private void showSearchSection() {
        defaultSections.setVisible(false);
        defaultSections.setManaged(false);
        searchSection.setVisible(true);
        searchSection.setManaged(true);
    }

    private void renderSection(HBox container, Label emptyLabel, AuctionDto[] auctions, String emptyMessage) {
        container.getChildren().clear();

        if (auctions == null || auctions.length == 0) {
            emptyLabel.setText(emptyMessage);
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        for (AuctionDto auction : auctions)
            container.getChildren().add(loadAuctionCard(auction));
    }

    private AnchorPane loadAuctionCard(AuctionDto auction) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-card.fxml"));
            AnchorPane card = loader.load();
            AuctionCardController controller = loader.getController();
            controller.bind(auction);
            renderedCardControllers.add(controller);
            return card;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load auction-card.fxml", e);
        }
    }

    private void cleanupRenderedCards() {
        for (AuctionCardController controller : renderedCardControllers)
            controller.cleanup();

        renderedCardControllers.clear();
    }

    private void search() {
        TextField searchBar = SceneManager.getMissionBarController().getSearchBar();
        String query = searchBar.getText();
        if (query == null || query.isBlank()) {
            loadHubSections();
            return;
        }

        try {
            AuctionDto[] results = auctionClientService.searchAuctions(query);
            searchResults = results == null ? new AuctionDto[0] : results;

            Platform.runLater(() -> {
                cleanupRenderedCards();
                showSearchSection();
                searchTitleLabel.setText("Results for '" + query + "'");
                renderSection(searchResultsContainer, searchEmptyStateLabel, searchResults,
                        "No auctions or sellers found matching '" + query + "'.");
            });
        } catch (IOException e) {
            logger.error("Search failed", e);
            Platform.runLater(() -> {
                cleanupRenderedCards();
                showSearchSection();
                searchTitleLabel.setText("Results for '" + query + "'");
                renderSection(searchResultsContainer, searchEmptyStateLabel, new AuctionDto[0],
                        "Search failed: Network error.");
            });
        } catch (AuctionException e) {
            Platform.runLater(() -> {
                cleanupRenderedCards();
                showSearchSection();
                searchTitleLabel.setText("Results for '" + query + "'");
                renderSection(searchResultsContainer, searchEmptyStateLabel, new AuctionDto[0], e.getMessage());
            });
        }
    }

    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.HOME, true, event -> search(), this::cleanup);
    }
}

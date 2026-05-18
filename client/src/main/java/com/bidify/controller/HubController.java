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

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class HubController {
    private static final Logger logger = LoggerFactory.getLogger(HubController.class);
    private static final double AUCTION_CARD_WIDTH = 460.0;
    private static final double AUCTION_ROW_SPACING = 24.0;
    private static final Duration ROW_SCROLL_DURATION = Duration.millis(220);

    @FXML
    private VBox defaultSections;

    @FXML
    private VBox searchSection;

    @FXML
    private HBox liveAuctionsContainer;

    @FXML
    private ScrollPane liveAuctionsScrollPane;

    @FXML
    private HBox upcomingAuctionsContainer;

    @FXML
    private ScrollPane upcomingAuctionsScrollPane;

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

    @FXML
    private Button liveScrollLeftButton;

    @FXML
    private Button liveScrollRightButton;

    @FXML
    private Button upcomingScrollLeftButton;

    @FXML
    private Button upcomingScrollRightButton;

    private AuctionDto[] liveAuctions = new AuctionDto[0];
    private AuctionDto[] upcomingAuctions = new AuctionDto[0];
    private AuctionDto[] searchResults = new AuctionDto[0];
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final List<AuctionCardController> renderedCardControllers = new ArrayList<>();

    @FXML
    private void initialize() {
        Platform.runLater(this::bindTopBar);
        setupRowNavigation(liveAuctionsScrollPane, liveScrollLeftButton, liveScrollRightButton);
        setupRowNavigation(upcomingAuctionsScrollPane, upcomingScrollLeftButton, upcomingScrollRightButton);
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
        resetNavigationForContainer(container);

        if (auctions == null || auctions.length == 0) {
            emptyLabel.setText(emptyMessage);
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            refreshNavigationForContainer(container);
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        for (AuctionDto auction : auctions)
            container.getChildren().add(loadAuctionCard(auction));

        refreshNavigationForContainer(container);
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

    @FXML
    private void scrollLiveLeft() {
        scrollRowByCard(liveAuctionsScrollPane, liveScrollLeftButton, liveScrollRightButton, -1);
    }

    @FXML
    private void scrollLiveRight() {
        scrollRowByCard(liveAuctionsScrollPane, liveScrollLeftButton, liveScrollRightButton, 1);
    }

    @FXML
    private void scrollUpcomingLeft() {
        scrollRowByCard(upcomingAuctionsScrollPane, upcomingScrollLeftButton, upcomingScrollRightButton, -1);
    }

    @FXML
    private void scrollUpcomingRight() {
        scrollRowByCard(upcomingAuctionsScrollPane, upcomingScrollLeftButton, upcomingScrollRightButton, 1);
    }

    private void setupRowNavigation(ScrollPane scrollPane, Button leftButton, Button rightButton) {
        scrollPane.hvalueProperty().addListener((observable, oldValue, newValue) ->
                updateRowNavigationButtons(scrollPane, leftButton, rightButton));
        scrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) ->
                Platform.runLater(() -> updateRowNavigationButtons(scrollPane, leftButton, rightButton)));
        if (scrollPane.getContent() != null)
            scrollPane.getContent().layoutBoundsProperty().addListener((observable, oldValue, newValue) ->
                    Platform.runLater(() -> updateRowNavigationButtons(scrollPane, leftButton, rightButton)));
    }

    private void resetNavigationForContainer(HBox container) {
        if (container == liveAuctionsContainer)
            liveAuctionsScrollPane.setHvalue(0);
        else if (container == upcomingAuctionsContainer)
            upcomingAuctionsScrollPane.setHvalue(0);
    }

    private void refreshNavigationForContainer(HBox container) {
        Platform.runLater(() -> {
            if (container == liveAuctionsContainer)
                updateRowNavigationButtons(liveAuctionsScrollPane, liveScrollLeftButton, liveScrollRightButton);
            else if (container == upcomingAuctionsContainer)
                updateRowNavigationButtons(upcomingAuctionsScrollPane, upcomingScrollLeftButton,
                        upcomingScrollRightButton);
        });
    }

    void scrollRowByCard(ScrollPane scrollPane, Button leftButton, Button rightButton, int direction) {
        if (scrollPane.getContent() == null) return;

        double targetHValue = calculateTargetHValue(scrollPane.getHvalue(), scrollPane.getContent().getLayoutBounds().getWidth(),
                scrollPane.getViewportBounds().getWidth(), direction * (AUCTION_CARD_WIDTH + AUCTION_ROW_SPACING));
        if (Double.compare(targetHValue, scrollPane.getHvalue()) == 0) {
            updateRowNavigationButtons(scrollPane, leftButton, rightButton);
            return;
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(scrollPane.hvalueProperty(), scrollPane.getHvalue())),
                new KeyFrame(ROW_SCROLL_DURATION, new KeyValue(scrollPane.hvalueProperty(), targetHValue)));
        timeline.setOnFinished(event -> updateRowNavigationButtons(scrollPane, leftButton, rightButton));
        timeline.play();
    }

    void updateRowNavigationButtons(ScrollPane scrollPane, Button leftButton, Button rightButton) {
        if (scrollPane.getContent() == null || !hasHorizontalOverflow(scrollPane.getContent().getLayoutBounds().getWidth(),
                scrollPane.getViewportBounds().getWidth())) {
            leftButton.setDisable(true);
            rightButton.setDisable(true);
            return;
        }

        double hValue = scrollPane.getHvalue();
        leftButton.setDisable(hValue <= 0.001);
        rightButton.setDisable(hValue >= 0.999);
    }

    static double calculateTargetHValue(double currentHValue, double contentWidth, double viewportWidth,
            double pixelDelta) {
        double scrollableWidth = contentWidth - viewportWidth;
        if (scrollableWidth <= 0) return 0;

        double currentPixel = currentHValue * scrollableWidth;
        double targetPixel = clamp(currentPixel + pixelDelta, 0, scrollableWidth);
        return targetPixel / scrollableWidth;
    }

    static boolean hasHorizontalOverflow(double contentWidth, double viewportWidth) {
        return contentWidth > viewportWidth + 1;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

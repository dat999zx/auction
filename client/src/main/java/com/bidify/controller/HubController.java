package com.bidify.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.Event;
import com.bidify.event.EventManager;
import com.bidify.service.AuctionClientService;
import com.bidify.common.utility.JsonUtil;
import com.bidify.navigation.CleanableController;
import com.bidify.ui.HubAuctionPatcher;
import com.bidify.ui.HubAuctionSectionRenderer;
import com.bidify.navigation.MissionBarUtil;
import com.bidify.navigation.NavPage;
import com.bidify.navigation.SceneManager;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class HubController implements CleanableController {
    private static final Logger logger = LoggerFactory.getLogger(HubController.class);
    private static final double AUCTION_CARD_WIDTH = 460.0;
    private static final double AUCTION_ROW_SPACING = 24.0;
    private static final Duration ROW_SCROLL_DURATION = Duration.millis(220);
    private static final String SORT_POPULARITY = "Popularity";
    private static final String SORT_ENDING_SOON = "Ending Soon";
    private static final String SORT_NEWEST = "Newest";

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
    @FXML
    private ComboBox<String> liveSortComboBox;
    @FXML
    private ComboBox<String> searchSortComboBox;

    private AuctionDto[] liveAuctions = new AuctionDto[0];
    private AuctionDto[] upcomingAuctions = new AuctionDto[0];
    private AuctionDto[] searchResults = new AuctionDto[0];
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private final Map<String, AuctionCardController> liveCardControllers = new HashMap<>();
    private final Map<String, AuctionCardController> upcomingCardControllers = new HashMap<>();
    private final Map<String, AuctionCardController> searchCardControllers = new HashMap<>();
    private String currentLiveSort = SORT_POPULARITY;
    private String currentSearchSort = SORT_NEWEST;

    @FXML
    private void initialize() {
        Platform.runLater(this::bindTopBar);
        setupSortControls();
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
        if (event == null) return;

        Platform.runLater(() -> {
            if (event.getType() == EventType.AUCTION_CREATED
                    || event.getType() == EventType.AUCTION_DELETED
                    || event.getType() == EventType.AUCTION_ENDED) {
                loadHubSections();
                return;
            }

            if (event.getType() != EventType.AUCTION_UPDATED && event.getType() != EventType.BID_PLACED)
                return;

            AuctionDto updatedAuction = JsonUtil.fromMap(event.getData(), AuctionDto.class);
            if (updatedAuction == null || updatedAuction.getId() == null || updatedAuction.getId().isBlank()) {
                loadHubSections();
                return;
            }

            if (relocateAuctionIfNeeded(updatedAuction))
                return;

            if (!"ACTIVE".equalsIgnoreCase(updatedAuction.getStatus())) {
                loadHubSections();
                return;
            }

            patchAuction(updatedAuction);
        });
    }

    // Hủy đăng ký lắng nghe sự kiện khi rời màn hình để tránh rò rỉ bộ nhớ.
    public void cleanup() {
        cleanupAllRenderedCards();
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
                showDefaultSections();
                renderLiveSection();
                renderUpcomingSection();
            });
        } catch (IOException e) {
            logger.error("Failed to load hub auctions", e);
            Platform.runLater(() -> {
                showDefaultSections();
                renderSection(liveAuctionsContainer, liveEmptyStateLabel, new AuctionDto[0], liveCardControllers, "Cannot connect to server.");
                renderSection(upcomingAuctionsContainer, upcomingEmptyStateLabel, new AuctionDto[0], upcomingCardControllers, "Cannot connect to server.");
            });
        } catch (AuctionException e) {
            Platform.runLater(() -> {
                showDefaultSections();
                renderSection(liveAuctionsContainer, liveEmptyStateLabel, new AuctionDto[0], liveCardControllers, e.getMessage());
                renderSection(upcomingAuctionsContainer, upcomingEmptyStateLabel, new AuctionDto[0], upcomingCardControllers, "No upcoming auctions right now.");
            });
        }
    }

    private void showDefaultSections() {
        defaultSections.setVisible(true);
        defaultSections.setManaged(true);
        searchSection.setVisible(false);
        searchSection.setManaged(false);
        HubAuctionSectionRenderer.cleanupControllers(searchCardControllers);
        searchResultsContainer.getChildren().clear();
    }

    private void showSearchSection() {
        defaultSections.setVisible(false);
        defaultSections.setManaged(false);
        searchSection.setVisible(true);
        searchSection.setManaged(true);
    }

    private void renderSection(HBox container, Label emptyLabel, AuctionDto[] auctions,
            Map<String, AuctionCardController> controllerMap, String emptyMessage) {
        HubAuctionSectionRenderer.renderSection(
                container,
                emptyLabel,
                auctions,
                controllerMap,
                emptyMessage,
                () -> resetNavigationForContainer(container),
                () -> refreshNavigationForContainer(container)
        );
    }

    private void cleanupAllRenderedCards() {
        HubAuctionSectionRenderer.cleanupControllers(liveCardControllers);
        HubAuctionSectionRenderer.cleanupControllers(upcomingCardControllers);
        HubAuctionSectionRenderer.cleanupControllers(searchCardControllers);
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
                showSearchSection();
                searchTitleLabel.setText("Results for '" + query + "'");
                renderSearchSection("No auctions or sellers found matching '" + query + "'.");
            });
        } catch (IOException e) {
            logger.error("Search failed", e);
            Platform.runLater(() -> {
                showSearchSection();
                searchTitleLabel.setText("Results for '" + query + "'");
                renderSection(searchResultsContainer, searchEmptyStateLabel, new AuctionDto[0], searchCardControllers, "Search failed: Network error.");
            });
        } catch (AuctionException e) {
            Platform.runLater(() -> {
                showSearchSection();
                searchTitleLabel.setText("Results for '" + query + "'");
                renderSection(searchResultsContainer, searchEmptyStateLabel, new AuctionDto[0], searchCardControllers, e.getMessage());
            });
        }
    }

    @FXML
    private void handleLiveSortChanged() {
        if (liveSortComboBox == null || liveSortComboBox.getValue() == null)
            return;

        currentLiveSort = liveSortComboBox.getValue();
        renderLiveSection();
    }

    @FXML
    private void handleSearchSortChanged() {
        if (searchSortComboBox == null || searchSortComboBox.getValue() == null)
            return;

        currentSearchSort = searchSortComboBox.getValue();
        if (searchSection.isVisible())
            renderSearchSection("No auctions found.");
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
        double targetPixel = Math.clamp(currentPixel + pixelDelta, 0, scrollableWidth);
        return targetPixel / scrollableWidth;
    }

    static boolean hasHorizontalOverflow(double contentWidth, double viewportWidth) {
        return contentWidth > viewportWidth + 1;
    }

    private void setupSortControls() {
        if (liveSortComboBox != null) {
            liveSortComboBox.getItems().setAll(SORT_POPULARITY, SORT_ENDING_SOON, SORT_NEWEST);
            liveSortComboBox.setValue(currentLiveSort);
        }
        if (searchSortComboBox != null) {
            searchSortComboBox.getItems().setAll(SORT_NEWEST);
            searchSortComboBox.setValue(currentSearchSort);
        }
    }

    private void renderLiveSection() {
        renderSection(liveAuctionsContainer, liveEmptyStateLabel, sortLiveAuctions(liveAuctions), liveCardControllers, "No live auctions right now.");
    }

    private void renderUpcomingSection() {
        renderSection(upcomingAuctionsContainer, upcomingEmptyStateLabel, upcomingAuctions, upcomingCardControllers, "No upcoming auctions right now.");
    }

    private void renderSearchSection(String emptyMessage) {
        renderSection(searchResultsContainer, searchEmptyStateLabel, sortSearchResults(searchResults), searchCardControllers, emptyMessage);
    }

    private AuctionDto[] sortLiveAuctions(AuctionDto[] source) {
        List<AuctionDto> auctions = new ArrayList<>(Arrays.asList(source == null ? new AuctionDto[0] : source));
        Comparator<AuctionDto> comparator = switch (currentLiveSort) {
            case SORT_ENDING_SOON -> Comparator
                    .comparing(this::parseEndTime, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(AuctionDto::getCurrentBid, Comparator.reverseOrder());
            case SORT_NEWEST -> Comparator
                    .comparing(this::parseCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator
                    .comparingInt(AuctionDto::getWatcherCount).reversed()
                    .thenComparing(Comparator.comparingInt(AuctionDto::getActiveBidderCount).reversed())
                    .thenComparing(Comparator.comparingDouble(AuctionDto::getCurrentBid).reversed())
                    .thenComparing(this::parseCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        auctions.sort(comparator);
        return auctions.toArray(AuctionDto[]::new);
    }

    private AuctionDto[] sortSearchResults(AuctionDto[] source) {
        List<AuctionDto> auctions = new ArrayList<>(Arrays.asList(source == null ? new AuctionDto[0] : source));
        auctions.sort(Comparator.comparing(this::parseCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return auctions.toArray(AuctionDto[]::new);
    }

    private void patchAuction(AuctionDto updatedAuction) {
        boolean updatedLive = HubAuctionPatcher.replaceAuction(liveAuctions, updatedAuction);
        if (updatedLive) {
            if (SORT_NEWEST.equals(currentLiveSort))
                patchCard(liveCardControllers, updatedAuction);
            else
                renderLiveSection();
        }

        if (HubAuctionPatcher.replaceAuction(searchResults, updatedAuction))
            patchCard(searchCardControllers, updatedAuction);
    }

    private boolean relocateAuctionIfNeeded(AuctionDto updatedAuction) {
        String auctionId = updatedAuction.getId();
        if (auctionId == null || auctionId.isBlank())
            return false;

        boolean isActive = "ACTIVE".equalsIgnoreCase(updatedAuction.getStatus());
        boolean isUpcoming = "UPCOMING".equalsIgnoreCase(updatedAuction.getStatus());
        boolean existsInLive = HubAuctionPatcher.containsAuction(liveAuctions, auctionId);
        boolean existsInUpcoming = HubAuctionPatcher.containsAuction(upcomingAuctions, auctionId);

        if (isActive && existsInUpcoming) {
            upcomingAuctions = HubAuctionPatcher.removeAuction(upcomingAuctions, auctionId);
            liveAuctions = HubAuctionPatcher.upsertAuction(liveAuctions, updatedAuction);
            renderUpcomingSection();
            renderLiveSection();
            patchSearchResult(updatedAuction);
            return true;
        }

        if (isUpcoming && existsInLive) {
            liveAuctions = HubAuctionPatcher.removeAuction(liveAuctions, auctionId);
            upcomingAuctions = HubAuctionPatcher.upsertAuction(upcomingAuctions, updatedAuction);
            renderLiveSection();
            renderUpcomingSection();
            patchSearchResult(updatedAuction);
            return true;
        }

        return false;
    }

    private void patchSearchResult(AuctionDto updatedAuction) {
        if (HubAuctionPatcher.replaceAuction(searchResults, updatedAuction))
            patchCard(searchCardControllers, updatedAuction);
    }

    private void patchCard(Map<String, AuctionCardController> controllerMap, AuctionDto updatedAuction) {
        AuctionCardController controller = controllerMap.get(updatedAuction.getId());
        if (controller != null)
            controller.bind(updatedAuction);
    }

    private LocalDateTime parseCreatedAt(AuctionDto auction) {
        return parseDateTime(auction == null ? null : auction.getCreatedAt());
    }

    private LocalDateTime parseEndTime(AuctionDto auction) {
        return parseDateTime(auction == null ? null : auction.getEndTime());
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank())
            return null;
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}

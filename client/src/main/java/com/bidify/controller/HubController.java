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
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.SceneManager;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
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

    // dùng để khởi tạo
    @FXML
    private void initialize() {
        Platform.runLater(this::bindTopBar);
        // dùng để setup sort controls
        setupSortControls();
        // dùng để setup dòng hiển thị navigation
        setupRowNavigation(liveAuctionsScrollPane, liveScrollLeftButton, liveScrollRightButton);
        // dùng để setup dòng hiển thị navigation
        setupRowNavigation(upcomingAuctionsScrollPane, upcomingScrollLeftButton, upcomingScrollRightButton);
        // dùng để tải hub sections
        loadHubSections();

        EventManager.getInstance().subscribe(EventType.AUCTION_CREATED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_UPDATED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_DELETED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.AUCTION_ENDED, this::handleAuctionEvent);
        EventManager.getInstance().subscribe(EventType.BID_PLACED, this::handleAuctionEvent);
    }

    // dùng để xử lý đấu giá sự kiện
    private void handleAuctionEvent(Event event) {
        if (event == null) return;

        Platform.runLater(() -> {
            if (event.getType() == EventType.AUCTION_CREATED
                    || event.getType() == EventType.AUCTION_DELETED
                    || event.getType() == EventType.AUCTION_ENDED) {
                // dùng để tải hub sections
                loadHubSections();
                return;
            }

            if (event.getType() != EventType.AUCTION_UPDATED && event.getType() != EventType.BID_PLACED)
                return;

            AuctionDto updatedAuction = JsonUtil.fromMap(event.getData(), AuctionDto.class);
            if (updatedAuction == null || updatedAuction.getId() == null || updatedAuction.getId().isBlank()) {
                // dùng để tải hub sections
                loadHubSections();
                return;
            }

            if (relocateAuctionIfNeeded(updatedAuction))
                return;

            if (!"ACTIVE".equalsIgnoreCase(updatedAuction.getStatus())) {
                // dùng để tải hub sections
                loadHubSections();
                return;
            }

            // dùng để patch đấu giá
            patchAuction(updatedAuction);
        });
    }

    // dùng để dọn dẹp tài nguyên
    public void cleanup() {
        // dùng để dọn dẹp tài nguyên all rendered cards
        cleanupAllRenderedCards();
        EventManager.getInstance().unsubscribe(EventType.AUCTION_CREATED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_UPDATED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_DELETED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.AUCTION_ENDED, this::handleAuctionEvent);
        EventManager.getInstance().unsubscribe(EventType.BID_PLACED, this::handleAuctionEvent);
    }

    // dùng để tải hub sections
    private void loadHubSections() {
        try {
            AuctionDto[] live = auctionClientService.getLiveAuctions();
            AuctionDto[] upcoming = auctionClientService.getUpcomingAuctions();

            liveAuctions = live == null ? new AuctionDto[0] : live;
            upcomingAuctions = upcoming == null ? new AuctionDto[0] : upcoming;

            Platform.runLater(() -> {
                // dùng để hiển thị default sections
                showDefaultSections();
                // dùng để hiển thị live section
                renderLiveSection();
                // dùng để hiển thị upcoming section
                renderUpcomingSection();
            });
        } catch (IOException e) {
            logger.error("Failed to load hub auctions", e);
            Platform.runLater(() -> {
                // dùng để hiển thị default sections
                showDefaultSections();
                // dùng để hiển thị section
                renderSection(liveAuctionsContainer, liveEmptyStateLabel, new AuctionDto[0], liveCardControllers, "Cannot connect to server.");
                // dùng để hiển thị section
                renderSection(upcomingAuctionsContainer, upcomingEmptyStateLabel, new AuctionDto[0], upcomingCardControllers, "Cannot connect to server.");
            });
        } catch (AuctionException e) {
            Platform.runLater(() -> {
                // dùng để hiển thị default sections
                showDefaultSections();
                renderSection(liveAuctionsContainer, liveEmptyStateLabel, new AuctionDto[0], liveCardControllers, e.getMessage());
                // dùng để hiển thị section
                renderSection(upcomingAuctionsContainer, upcomingEmptyStateLabel, new AuctionDto[0], upcomingCardControllers, "No upcoming auctions right now.");
            });
        }
    }

    // dùng để hiển thị default sections
    private void showDefaultSections() {
        defaultSections.setVisible(true);
        defaultSections.setManaged(true);
        searchSection.setVisible(false);
        searchSection.setManaged(false);
        // dùng để dọn dẹp tài nguyên controllers
        cleanupControllers(searchCardControllers);
        searchResultsContainer.getChildren().clear();
    }

    // dùng để hiển thị tìm kiếm section
    private void showSearchSection() {
        defaultSections.setVisible(false);
        defaultSections.setManaged(false);
        searchSection.setVisible(true);
        searchSection.setManaged(true);
    }

    private void renderSection(HBox container, Label emptyLabel, AuctionDto[] auctions,
            Map<String, AuctionCardController> controllerMap, String emptyMessage) {
        // dùng để dọn dẹp tài nguyên controllers
        cleanupControllers(controllerMap);
        container.getChildren().clear();
        // dùng để đặt lại navigation cho container
        resetNavigationForContainer(container);

        if (auctions == null || auctions.length == 0) {
            emptyLabel.setText(emptyMessage);
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            // dùng để refresh navigation cho container
            refreshNavigationForContainer(container);
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        for (AuctionDto auction : auctions)
            container.getChildren().add(loadAuctionCard(auction, controllerMap));

        // dùng để refresh navigation cho container
        refreshNavigationForContainer(container);
    }

    // dùng để tải đấu giá card
    private AnchorPane loadAuctionCard(AuctionDto auction, Map<String, AuctionCardController> controllerMap) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction-card.fxml"));
            AnchorPane card = loader.load();
            AuctionCardController controller = loader.getController();
            controller.bind(auction);
            if (auction != null && auction.getId() != null)
                controllerMap.put(auction.getId(), controller);
            return card;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load auction-card.fxml", e);
        }
    }

    // dùng để dọn dẹp tài nguyên controllers
    private void cleanupControllers(Map<String, AuctionCardController> controllerMap) {
        for (AuctionCardController controller : controllerMap.values())
            controller.cleanup();
        controllerMap.clear();
    }

    // dùng để dọn dẹp tài nguyên all rendered cards
    private void cleanupAllRenderedCards() {
        // dùng để dọn dẹp tài nguyên controllers
        cleanupControllers(liveCardControllers);
        // dùng để dọn dẹp tài nguyên controllers
        cleanupControllers(upcomingCardControllers);
        // dùng để dọn dẹp tài nguyên controllers
        cleanupControllers(searchCardControllers);
    }

    // dùng để tìm kiếm
    private void search() {
        TextField searchBar = SceneManager.getMissionBarController().getSearchBar();
        String query = searchBar.getText();
        if (query == null || query.isBlank()) {
            // dùng để tải hub sections
            loadHubSections();
            return;
        }

        try {
            AuctionDto[] results = auctionClientService.searchAuctions(query);
            searchResults = results == null ? new AuctionDto[0] : results;

            Platform.runLater(() -> {
                // dùng để hiển thị tìm kiếm section
                showSearchSection();
                searchTitleLabel.setText("Results for '" + query + "'");
                // dùng để hiển thị tìm kiếm section
                renderSearchSection("No auctions or sellers found matching '" + query + "'.");
            });
        } catch (IOException e) {
            logger.error("Search failed", e);
            Platform.runLater(() -> {
                // dùng để hiển thị tìm kiếm section
                showSearchSection();
                searchTitleLabel.setText("Results for '" + query + "'");
                // dùng để hiển thị section
                renderSection(searchResultsContainer, searchEmptyStateLabel, new AuctionDto[0], searchCardControllers, "Search failed: Network error.");
            });
        } catch (AuctionException e) {
            Platform.runLater(() -> {
                // dùng để hiển thị tìm kiếm section
                showSearchSection();
                searchTitleLabel.setText("Results for '" + query + "'");
                renderSection(searchResultsContainer, searchEmptyStateLabel, new AuctionDto[0], searchCardControllers, e.getMessage());
            });
        }
    }

    // dùng để xử lý live sort changed
    @FXML
    private void handleLiveSortChanged() {
        if (liveSortComboBox == null || liveSortComboBox.getValue() == null)
            return;

        currentLiveSort = liveSortComboBox.getValue();
        // dùng để hiển thị live section
        renderLiveSection();
    }

    // dùng để xử lý tìm kiếm sort changed
    @FXML
    private void handleSearchSortChanged() {
        if (searchSortComboBox == null || searchSortComboBox.getValue() == null)
            return;

        currentSearchSort = searchSortComboBox.getValue();
        if (searchSection.isVisible())
            // dùng để hiển thị tìm kiếm section
            renderSearchSection("No auctions found.");
    }

    // dùng để liên kết dữ liệu top bar
    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.HOME, true, event -> search(), this::cleanup);
    }

    // dùng để scroll live left
    @FXML
    private void scrollLiveLeft() {
        // dùng để scroll dòng hiển thị bởi card
        scrollRowByCard(liveAuctionsScrollPane, liveScrollLeftButton, liveScrollRightButton, -1);
    }

    // dùng để scroll live right
    @FXML
    private void scrollLiveRight() {
        // dùng để scroll dòng hiển thị bởi card
        scrollRowByCard(liveAuctionsScrollPane, liveScrollLeftButton, liveScrollRightButton, 1);
    }

    // dùng để scroll upcoming left
    @FXML
    private void scrollUpcomingLeft() {
        // dùng để scroll dòng hiển thị bởi card
        scrollRowByCard(upcomingAuctionsScrollPane, upcomingScrollLeftButton, upcomingScrollRightButton, -1);
    }

    // dùng để scroll upcoming right
    @FXML
    private void scrollUpcomingRight() {
        // dùng để scroll dòng hiển thị bởi card
        scrollRowByCard(upcomingAuctionsScrollPane, upcomingScrollLeftButton, upcomingScrollRightButton, 1);
    }

    // dùng để setup dòng hiển thị navigation
    private void setupRowNavigation(ScrollPane scrollPane, Button leftButton, Button rightButton) {
        scrollPane.hvalueProperty().addListener((observable, oldValue, newValue) ->
                updateRowNavigationButtons(scrollPane, leftButton, rightButton));
        scrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) ->
                Platform.runLater(() -> updateRowNavigationButtons(scrollPane, leftButton, rightButton)));
        if (scrollPane.getContent() != null)
            scrollPane.getContent().layoutBoundsProperty().addListener((observable, oldValue, newValue) ->
                    Platform.runLater(() -> updateRowNavigationButtons(scrollPane, leftButton, rightButton)));
    }

    // dùng để đặt lại navigation cho container
    private void resetNavigationForContainer(HBox container) {
        if (container == liveAuctionsContainer)
            liveAuctionsScrollPane.setHvalue(0);
        else if (container == upcomingAuctionsContainer)
            upcomingAuctionsScrollPane.setHvalue(0);
    }

    // dùng để refresh navigation cho container
    private void refreshNavigationForContainer(HBox container) {
        Platform.runLater(() -> {
            if (container == liveAuctionsContainer)
                // dùng để cập nhật dòng hiển thị navigation buttons
                updateRowNavigationButtons(liveAuctionsScrollPane, liveScrollLeftButton, liveScrollRightButton);
            else if (container == upcomingAuctionsContainer)
                updateRowNavigationButtons(upcomingAuctionsScrollPane, upcomingScrollLeftButton,
                        upcomingScrollRightButton);
        });
    }

    // dùng để scroll dòng hiển thị bởi card
    void scrollRowByCard(ScrollPane scrollPane, Button leftButton, Button rightButton, int direction) {
        if (scrollPane.getContent() == null) return;

        double targetHValue = calculateTargetHValue(scrollPane.getHvalue(), scrollPane.getContent().getLayoutBounds().getWidth(),
                scrollPane.getViewportBounds().getWidth(), direction * (AUCTION_CARD_WIDTH + AUCTION_ROW_SPACING));
        if (Double.compare(targetHValue, scrollPane.getHvalue()) == 0) {
            // dùng để cập nhật dòng hiển thị navigation buttons
            updateRowNavigationButtons(scrollPane, leftButton, rightButton);
            return;
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(scrollPane.hvalueProperty(), scrollPane.getHvalue())),
                new KeyFrame(ROW_SCROLL_DURATION, new KeyValue(scrollPane.hvalueProperty(), targetHValue)));
        timeline.setOnFinished(event -> updateRowNavigationButtons(scrollPane, leftButton, rightButton));
        timeline.play();
    }

    // dùng để cập nhật dòng hiển thị navigation buttons
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

    // dùng để kiểm tra xem có horizontal overflow
    static boolean hasHorizontalOverflow(double contentWidth, double viewportWidth) {
        return contentWidth > viewportWidth + 1;
    }

    // dùng để clamp
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // dùng để setup sort controls
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

    // dùng để hiển thị live section
    private void renderLiveSection() {
        renderSection(liveAuctionsContainer, liveEmptyStateLabel, sortLiveAuctions(liveAuctions), liveCardControllers, "No live auctions right now.");
    }

    // dùng để hiển thị upcoming section
    private void renderUpcomingSection() {
        // dùng để hiển thị section
        renderSection(upcomingAuctionsContainer, upcomingEmptyStateLabel, upcomingAuctions, upcomingCardControllers, "No upcoming auctions right now.");
    }

    // dùng để hiển thị tìm kiếm section
    private void renderSearchSection(String emptyMessage) {
        renderSection(searchResultsContainer, searchEmptyStateLabel, sortSearchResults(searchResults), searchCardControllers, emptyMessage);
    }

    // dùng để sort live danh sách đấu giá
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

    // dùng để sort tìm kiếm results
    private AuctionDto[] sortSearchResults(AuctionDto[] source) {
        List<AuctionDto> auctions = new ArrayList<>(Arrays.asList(source == null ? new AuctionDto[0] : source));
        auctions.sort(Comparator.comparing(this::parseCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return auctions.toArray(AuctionDto[]::new);
    }

    // dùng để patch đấu giá
    private void patchAuction(AuctionDto updatedAuction) {
        boolean updatedLive = replaceAuction(liveAuctions, updatedAuction);
        if (updatedLive) {
            if (SORT_NEWEST.equals(currentLiveSort))
                // dùng để patch card
                patchCard(liveCardControllers, updatedAuction);
            else
                // dùng để hiển thị live section
                renderLiveSection();
        }

        if (replaceAuction(searchResults, updatedAuction))
            // dùng để patch card
            patchCard(searchCardControllers, updatedAuction);
    }

    // dùng để relocate đấu giá if needed
    private boolean relocateAuctionIfNeeded(AuctionDto updatedAuction) {
        String auctionId = updatedAuction.getId();
        if (auctionId == null || auctionId.isBlank())
            return false;

        boolean isActive = "ACTIVE".equalsIgnoreCase(updatedAuction.getStatus());
        boolean isUpcoming = "UPCOMING".equalsIgnoreCase(updatedAuction.getStatus());
        boolean existsInLive = containsAuction(liveAuctions, auctionId);
        boolean existsInUpcoming = containsAuction(upcomingAuctions, auctionId);

        if (isActive && existsInUpcoming) {
            upcomingAuctions = removeAuction(upcomingAuctions, auctionId);
            liveAuctions = upsertAuction(liveAuctions, updatedAuction);
            // dùng để hiển thị upcoming section
            renderUpcomingSection();
            // dùng để hiển thị live section
            renderLiveSection();
            // dùng để patch tìm kiếm result
            patchSearchResult(updatedAuction);
            return true;
        }

        if (isUpcoming && existsInLive) {
            liveAuctions = removeAuction(liveAuctions, auctionId);
            upcomingAuctions = upsertAuction(upcomingAuctions, updatedAuction);
            // dùng để hiển thị live section
            renderLiveSection();
            // dùng để hiển thị upcoming section
            renderUpcomingSection();
            // dùng để patch tìm kiếm result
            patchSearchResult(updatedAuction);
            return true;
        }

        return false;
    }

    // dùng để patch tìm kiếm result
    private void patchSearchResult(AuctionDto updatedAuction) {
        if (replaceAuction(searchResults, updatedAuction))
            // dùng để patch card
            patchCard(searchCardControllers, updatedAuction);
    }

    // dùng để replace đấu giá
    private boolean replaceAuction(AuctionDto[] auctions, AuctionDto updatedAuction) {
        if (auctions == null || updatedAuction == null || updatedAuction.getId() == null)
            return false;

        for (int i = 0; i < auctions.length; i++) {
            AuctionDto existing = auctions[i];
            if (existing == null || existing.getId() == null)
                continue;
            if (!updatedAuction.getId().equals(existing.getId()))
                continue;
            auctions[i] = updatedAuction;
            return true;
        }

        return false;
    }

    // dùng để contains đấu giá
    private boolean containsAuction(AuctionDto[] auctions, String auctionId) {
        if (auctions == null || auctionId == null || auctionId.isBlank())
            return false;

        for (AuctionDto auction : auctions) {
            if (auction == null || auction.getId() == null)
                continue;
            if (auctionId.equals(auction.getId()))
                return true;
        }

        return false;
    }

    // dùng để xóa đấu giá
    private AuctionDto[] removeAuction(AuctionDto[] auctions, String auctionId) {
        if (auctions == null || auctions.length == 0 || auctionId == null || auctionId.isBlank())
            return new AuctionDto[0];

        List<AuctionDto> remaining = new ArrayList<>();
        for (AuctionDto auction : auctions) {
            if (auction == null || auction.getId() == null || auctionId.equals(auction.getId()))
                continue;
            remaining.add(auction);
        }
        return remaining.toArray(AuctionDto[]::new);
    }

    // dùng để upsert đấu giá
    private AuctionDto[] upsertAuction(AuctionDto[] auctions, AuctionDto updatedAuction) {
        if (updatedAuction == null || updatedAuction.getId() == null || updatedAuction.getId().isBlank())
            return auctions == null ? new AuctionDto[0] : auctions;

        List<AuctionDto> next = new ArrayList<>(Arrays.asList(auctions == null ? new AuctionDto[0] : auctions));
        for (int i = 0; i < next.size(); i++) {
            AuctionDto existing = next.get(i);
            if (existing == null || existing.getId() == null)
                continue;
            if (!updatedAuction.getId().equals(existing.getId()))
                continue;
            next.set(i, updatedAuction);
            return next.toArray(AuctionDto[]::new);
        }

        next.add(updatedAuction);
        return next.toArray(AuctionDto[]::new);
    }

    // dùng để patch card
    private void patchCard(Map<String, AuctionCardController> controllerMap, AuctionDto updatedAuction) {
        AuctionCardController controller = controllerMap.get(updatedAuction.getId());
        if (controller != null)
            controller.bind(updatedAuction);
    }

    // dùng để phân tích cú pháp created tại
    private LocalDateTime parseCreatedAt(AuctionDto auction) {
        return parseDateTime(auction == null ? null : auction.getCreatedAt());
    }

    // dùng để phân tích cú pháp end thời gian
    private LocalDateTime parseEndTime(AuctionDto auction) {
        return parseDateTime(auction == null ? null : auction.getEndTime());
    }

    // dùng để phân tích cú pháp ngày thời gian
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

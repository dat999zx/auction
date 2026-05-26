package com.bidify.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionResolutionAction;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.model.ClientSession;
import com.bidify.service.AdminClientService;
import com.bidify.service.AuctionClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;
import com.bidify.utility.SoundUtil;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AdminAuctionsController {
    private final AdminClientService adminClientService = new AdminClientService();
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private List<AuctionDto> allAuctions = new ArrayList<>();

    @FXML private VBox auctionsContainer;
    @FXML private Label summaryLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            if (!ClientSession.getInstance().isAdmin()) {
                NotificationUtil.error("Only admins can access this page.");
                SceneManager.switchScene("hub.fxml", false, true);
                return;
            }

            MissionBarUtil.setup(NavPage.ADMIN_AUCTIONS, false, null);
            setupFilters();
            loadAuctions();
        });
    }

    @FXML
    private void handleRefresh() {
        loadAuctions();
    }

    @FXML
    private void handleFilterChanged() {
        renderAuctions();
    }

    private void setupFilters() {
        statusFilter.getItems().setAll("All statuses", "ACTIVE", "UPCOMING", "AWAITING_PAYMENT", "AWAITING_DELIVERY", "COMPLETED", "CANCELED");
        statusFilter.getSelectionModel().selectFirst();
    }

    private void loadAuctions() {
        try {
            allAuctions = adminClientService.getAdminAuctions();
            renderAuctions();
        } catch (IOException e) {
            allAuctions = new ArrayList<>();
            auctionsContainer.getChildren().clear();
            summaryLabel.setText("Cannot connect to server.");
            NotificationUtil.error("Cannot connect to server.");
        } catch (ValidationException e) {
            allAuctions = new ArrayList<>();
            auctionsContainer.getChildren().clear();
            summaryLabel.setText(e.getMessage());
            NotificationUtil.error(e.getMessage());
        }
    }

    private void renderAuctions() {
        auctionsContainer.getChildren().clear();
        List<AuctionDto> filtered = filterAuctions();

        if (filtered.isEmpty()) {
            summaryLabel.setText("No auctions found");
            auctionsContainer.getChildren().add(createEmptyState());
            return;
        }

        summaryLabel.setText("Showing " + filtered.size() + " of " + allAuctions.size() + " auctions");
        for (AuctionDto auction : filtered)
            auctionsContainer.getChildren().add(createAuctionRow(auction));
    }

    private List<AuctionDto> filterAuctions() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();
        List<AuctionDto> filtered = new ArrayList<>();

        for (AuctionDto auction : allAuctions) {
            String auctionStatus = safe(auction.getStatus()).toUpperCase();
            boolean statusMatches = status == null || "All statuses".equals(status) || status.equals(auctionStatus);
            boolean queryMatches = query.isBlank()
                || safe(auction.getAuctionName()).toLowerCase().contains(query)
                || safe(auction.getSellerUsername()).toLowerCase().contains(query)
                || safe(auction.getCurrentBidderUsername()).toLowerCase().contains(query)
                || safe(auction.getCategory()).toLowerCase().contains(query);

            if (statusMatches && queryMatches)
                filtered.add(auction);
        }

        return filtered;
    }

    private VBox createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().add("admin-empty-state");
        Label title = new Label("No auctions match the current filters.");
        title.getStyleClass().add("admin-auction-title");
        Label subtitle = new Label("Adjust the search text or status filter.");
        subtitle.getStyleClass().add("admin-auction-copy");
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private HBox createAuctionRow(AuctionDto auction) {
        HBox row = new HBox(12);
        row.getStyleClass().add("admin-auction-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));

        VBox details = new VBox(4);
        HBox.setHgrow(details, Priority.ALWAYS);

        Label title = new Label(DisplayUtil.defaultText(auction.getAuctionName(), "Untitled auction"));
        title.getStyleClass().add("admin-auction-title");

        Label subtitle = new Label(buildSubtitle(auction));
        subtitle.getStyleClass().add("admin-auction-copy");

        details.getChildren().addAll(title, subtitle);

        Label price = new Label(DisplayUtil.formatCurrency(resolveDisplayPrice(auction)));
        price.getStyleClass().add("admin-auction-copy");

        Label status = new Label(safe(auction.getStatus()).toUpperCase());
        status.getStyleClass().addAll("admin-status-pill", statusClass(auction));

        Button openButton = new Button("Open");
        openButton.getStyleClass().add("admin-action-button");
        openButton.setOnAction(event -> openAuctionDetail(auction));

        row.getChildren().addAll(details, price, status, openButton);
        addActionButtons(row, auction);
        return row;
    }

    private void addActionButtons(HBox row, AuctionDto auction) {
        String status = safe(auction.getStatus()).toUpperCase();

        if ("AWAITING_DELIVERY".equals(status)) {
            Button completeButton = new Button("Complete");
            completeButton.getStyleClass().add("admin-action-button");
            completeButton.setOnAction(event -> resolveAuction(auction, AuctionResolutionAction.COMPLETE));
            row.getChildren().add(completeButton);
        }

        if ("ACTIVE".equals(status) || "UPCOMING".equals(status)) {
            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().addAll("admin-action-button", "admin-danger-button");
            cancelButton.setOnAction(event -> cancelAuction(auction));
            row.getChildren().add(cancelButton);
        } else if ("AWAITING_PAYMENT".equals(status) || "AWAITING_DELIVERY".equals(status)) {
            Button cancelButton = new Button("Cancel");
            cancelButton.getStyleClass().addAll("admin-action-button", "admin-danger-button");
            cancelButton.setOnAction(event -> resolveAuction(auction, AuctionResolutionAction.CANCEL));
            row.getChildren().add(cancelButton);
        }
    }

    private void cancelAuction(AuctionDto auction) {
        try {
            auctionClientService.deleteAuction(auction.getId());
            SoundUtil.success();
            NotificationUtil.success("Auction canceled successfully.");
            loadAuctions();
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    private void resolveAuction(AuctionDto auction, AuctionResolutionAction action) {
        try {
            auctionClientService.resolveAuction(auction.getId(), action);
            SoundUtil.success();
            NotificationUtil.success("Auction resolved successfully.");
            loadAuctions();
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
        } catch (AuctionException e) {
            NotificationUtil.error(e.getMessage());
        }
    }

    private void openAuctionDetail(AuctionDto auction) {
        AuctionDetailsController.setAuctionId(auction.getId());
        SceneManager.clearCache("auctiondetail.fxml");
        SceneManager.switchScene("auctiondetail.fxml", false, true);
    }

    private String buildSubtitle(AuctionDto auction) {
        List<String> parts = new ArrayList<>();
        parts.add("Seller: " + DisplayUtil.defaultText(auction.getSellerUsername(), "Unknown"));
        if (!safe(auction.getCurrentBidderUsername()).isBlank())
            parts.add("Leader: " + auction.getCurrentBidderUsername());
        if (!safe(auction.getCategory()).isBlank())
            parts.add(auction.getCategory());
        return String.join(" | ", parts);
    }

    private double resolveDisplayPrice(AuctionDto auction) {
        return auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice();
    }

    private String statusClass(AuctionDto auction) {
        return switch (safe(auction.getStatus()).toUpperCase()) {
            case "ACTIVE" -> "status-active";
            case "UPCOMING", "AWAITING_PAYMENT", "AWAITING_DELIVERY" -> "status-warning";
            case "CANCELED", "BANNED" -> "status-danger";
            default -> "status-closed";
        };
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

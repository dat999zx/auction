package com.bidify.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.service.AuctionClientService;
import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;
import com.bidify.utility.UiUpdateScheduler;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MyAuctionsController {
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private AuctionDto[] allAuctions = new AuctionDto[0];
    private final List<String> timerSubscriptionIds = new ArrayList<>();

    @FXML private VBox auctionRowsContainer;
    @FXML private Label summaryLabel;
    @FXML private Label statActiveVolume;
    @FXML private Label statParticipants;
    @FXML private Label statWinRate;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            MissionBarUtil.setup(NavPage.MY_AUCTIONS, true, null, this::cleanup);
            loadMyAuctions();
        });
    }

    private void cleanup() {
        for (String id : timerSubscriptionIds)
            UiUpdateScheduler.getInstance().unsubscribe(id);
        timerSubscriptionIds.clear();
    }

    private void loadMyAuctions() {
        try {
            AuctionDto[] auctions = auctionClientService.getMyAuctions();
            allAuctions = auctions == null ? new AuctionDto[0] : auctions;
            Platform.runLater(() -> {
                renderStats();
                renderRows();
            });
        } catch (IOException e) {
            allAuctions = new AuctionDto[0];
            Platform.runLater(() -> {
                renderStats();
                renderRows();
                NotificationUtil.error("Cannot connect to server.");
            });
        } catch (AuctionException e) {
            allAuctions = new AuctionDto[0];
            Platform.runLater(() -> {
                renderStats();
                renderRows();
                NotificationUtil.error(e.getMessage());
            });
        }
    }

    private void renderStats() {
        int activeCount = 0;
        double activeVolume = 0;
        int totalBids = 0;
        int wonCount = 0;
        int closedCount = 0;

        for (AuctionDto a : allAuctions) {
            String status = safe(a.getStatus()).toUpperCase();
            if ("ACTIVE".equals(status)) {
                activeCount++;
                activeVolume += a.getCurrentBid() > 0 ? a.getCurrentBid() : a.getStartingPrice();
            }
            if (a.getBidHistory() != null)
                totalBids += a.getBidHistory().size();

            if (isClosedStatus(status)) {
                closedCount++;
                if (a.getCurrentBidderUsername() != null && !a.getCurrentBidderUsername().isBlank())
                    wonCount++;
            }
        }

        if (statActiveVolume != null)
            statActiveVolume.setText(DisplayUtil.formatCurrency(activeVolume));
        if (statParticipants != null)
            statParticipants.setText(String.valueOf(totalBids));
        if (statWinRate != null) {
            String rate = closedCount > 0 ? ((wonCount * 100 / closedCount) + "%") : "N/A";
            statWinRate.setText(rate);
        }
    }

    private void renderRows() {
        cleanup();
        auctionRowsContainer.getChildren().clear();

        if (allAuctions.length == 0) {
            auctionRowsContainer.getChildren().add(createEmptyState());
            if (summaryLabel != null)
                summaryLabel.setText("No auctions yet");
            return;
        }

        for (AuctionDto auction : allAuctions)
            auctionRowsContainer.getChildren().add(createRow(auction));

        if (summaryLabel != null)
            summaryLabel.setText("Showing " + allAuctions.length + " of " + allAuctions.length + " auctions");
    }

    private Node createEmptyState() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(28, 32, 28, 32));
        box.getStyleClass().add("table-row");

        Label title = new Label("You have no auctions yet.");
        title.getStyleClass().add("product-title");

        Label subtitle = new Label("Create an auction from the sidebar to get started.");
        subtitle.getStyleClass().add("product-subtitle");
        subtitle.setWrapText(true);

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private Node createRow(AuctionDto auction) {
        String status = safe(auction.getStatus()).toUpperCase();

        GridPane row = new GridPane();
        row.setHgap(20);
        row.getStyleClass().add("table-row");
        row.setPadding(new Insets(13, 24, 13, 24));
        row.getColumnConstraints().addAll(
            col(38), col(14), col(16), col(20), col(12)
        );

        // Column 0: product details
        HBox details = new HBox(14);
        details.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().add(createThumbnail(auction));

        VBox textBox = new VBox(3);
        Label titleLabel = new Label(DisplayUtil.defaultText(auction.getAuctionName(), "Untitled"));
        titleLabel.getStyleClass().add("product-title");
        Label subtitleLabel = new Label(buildSubtitle(auction));
        subtitleLabel.getStyleClass().add("product-subtitle");
        textBox.getChildren().addAll(titleLabel, subtitleLabel);
        details.getChildren().add(textBox);
        row.add(details, 0, 0);

        // Column 1: status badge
        boolean endingSoon = "ACTIVE".equals(status) && isEndingSoon(auction.getEndTime());
        Label badge = new Label(mapStatusLabel(status, endingSoon));
        badge.getStyleClass().addAll("badge", mapBadgeClass(status, endingSoon));
        GridPane.setValignment(badge, javafx.geometry.VPos.CENTER);
        row.add(badge, 1, 0);

        // Column 2: current bid
        VBox bidBox = new VBox(2);
        bidBox.setAlignment(Pos.CENTER_LEFT);
        GridPane.setValignment(bidBox, javafx.geometry.VPos.CENTER);

        if ("UPCOMING".equals(status)) {
            Label dash = new Label("\u2014");
            dash.getStyleClass().add("price-value");
            Label cap = new Label("Not yet started");
            cap.getStyleClass().add("caption");
            bidBox.getChildren().addAll(dash, cap);
        } else {
            double price = auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice();
            Label priceLabel = new Label(DisplayUtil.formatCurrency(price));
            priceLabel.getStyleClass().add("price-value");
            String capText = isClosedStatus(status) ? "Final Price"
                    : (auction.getBidHistory() != null ? auction.getBidHistory().size() + " Bids total" : "0 Bids total");
            Label cap = new Label(capText);
            cap.getStyleClass().add("caption");
            bidBox.getChildren().addAll(priceLabel, cap);
        }
        row.add(bidBox, 2, 0);

        // Column 3: time remaining
        Label timeLabel = new Label();
        GridPane.setValignment(timeLabel, javafx.geometry.VPos.CENTER);
        if ("ACTIVE".equals(status)) {
            if (endingSoon) {
                timeLabel.getStyleClass().add("error-text");
                String subId = UiUpdateScheduler.getInstance()
                        .subscribe(() -> timeLabel.setText("\u23F1  " + DisplayUtil.formatRemainingTime(auction.getEndTime())));
                timerSubscriptionIds.add(subId);
            } else {
                timeLabel.getStyleClass().add("muted-text");
                String subId = UiUpdateScheduler.getInstance()
                        .subscribe(() -> timeLabel.setText("\u25F7  " + DisplayUtil.formatRemainingTime(auction.getEndTime())));
                timerSubscriptionIds.add(subId);
            }
        } else if ("UPCOMING".equals(status)) {
            timeLabel.setText("Scheduling...");
            timeLabel.getStyleClass().add("muted-text");
        } else if (isClosedStatus(status)) {
            timeLabel.setText("Ended " + formatEndDate(auction.getEndTime()));
            timeLabel.getStyleClass().add("muted-text");
        } else {
            timeLabel.setText(status);
            timeLabel.getStyleClass().add("muted-text");
        }
        row.add(timeLabel, 3, 0);

        // Column 4: actions
        GridPane.setValignment(row, javafx.geometry.VPos.CENTER);
        if ("UPCOMING".equals(status)) {
            Button editButton = new Button("Edit");
            editButton.getStyleClass().add("small-button");
            editButton.setOnAction(e -> openModifyAuction(auction));
            GridPane.setValignment(editButton, javafx.geometry.VPos.CENTER);
            row.add(editButton, 4, 0);
        } else if ("ACTIVE".equals(status)) {
            Button moreBtn = new Button("\u22EE");
            moreBtn.getStyleClass().add("more-button");
            moreBtn.setOnAction(e -> openAuctionDetail(auction));
            GridPane.setValignment(moreBtn, javafx.geometry.VPos.CENTER);
            row.add(moreBtn, 4, 0);
        } else {
            Button moreBtn = new Button("\u25A4");
            moreBtn.getStyleClass().add("more-button");
            moreBtn.setOnAction(e -> openAuctionDetail(auction));
            GridPane.setValignment(moreBtn, javafx.geometry.VPos.CENTER);
            row.add(moreBtn, 4, 0);
        }

        return row;
    }

    private ColumnConstraints col(double percent) {
        ColumnConstraints c = new ColumnConstraints();
        c.setPercentWidth(percent);
        return c;
    }

    private Node createThumbnail(AuctionDto auction) {
        StackPane frame = new StackPane();
        frame.setPrefSize(48, 48);
        frame.getStyleClass().add("product-image");

        Image image = decodeImage(auction.getThumbnailBase64());
        if (image != null) {
            ImageView iv = new ImageView(image);
            iv.setFitWidth(48);
            iv.setFitHeight(48);
            iv.setPreserveRatio(false);
            frame.getChildren().add(iv);
        } else {
            Label placeholder = new Label(resolveInitial(auction.getAuctionName()));
            placeholder.setStyle("-fx-font-weight: 700; -fx-text-fill: #454652;");
            frame.getChildren().add(placeholder);
        }
        return frame;
    }

    private Image decodeImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            return new Image(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String buildSubtitle(AuctionDto auction) {
        List<String> parts = new ArrayList<>();
        if (!safe(auction.getCategory()).isBlank()) parts.add(auction.getCategory());
        if (!safe(auction.getProductType()).isBlank()) parts.add(auction.getProductType());
        return parts.isEmpty() ? "No details" : String.join(" \u2022 ", parts);
    }

    private String mapStatusLabel(String status, boolean endingSoon) {
        if ("ACTIVE".equals(status) && endingSoon) return "ENDING SOON";
        return switch (status) {
            case "ACTIVE" -> "ACTIVE";
            case "UPCOMING" -> "DRAFT";
            case "ENDED", "AWAITING_PAYMENT", "AWAITING_DELIVERY", "COMPLETED", "PAID" -> "CLOSED";
            case "CANCELED" -> "CANCELED";
            case "BANNED" -> "BANNED";
            default -> status;
        };
    }

    private String mapBadgeClass(String status, boolean endingSoon) {
        if ("ACTIVE".equals(status) && endingSoon) return "badge-ending";
        return switch (status) {
            case "ACTIVE" -> "badge-active";
            case "UPCOMING" -> "badge-draft";
            case "ENDED", "AWAITING_PAYMENT", "AWAITING_DELIVERY", "COMPLETED", "PAID" -> "badge-closed";
            case "CANCELED", "BANNED" -> "badge-ending";
            default -> "badge-draft";
        };
    }

    private boolean isClosedStatus(String status) {
        return switch (status) {
            case "ENDED", "AWAITING_PAYMENT", "AWAITING_DELIVERY", "COMPLETED", "PAID", "CANCELED", "BANNED" -> true;
            default -> false;
        };
    }

    private boolean isEndingSoon(String endTime) {
        if (endTime == null || endTime.isBlank()) return false;
        try {
            Duration d = Duration.between(LocalDateTime.now(), LocalDateTime.parse(endTime));
            return !d.isNegative() && d.toHours() < 24;
        } catch (DateTimeParseException e) {
            return false;
        }
    }



    private String formatEndDate(String endTime) {
        if (endTime == null || endTime.isBlank()) return "";
        try {
            LocalDateTime dt = LocalDateTime.parse(endTime);
            return dt.format(DateTimeFormatter.ofPattern("MMM dd"));
        } catch (DateTimeParseException e) {
            return endTime;
        }
    }

    private void openModifyAuction(AuctionDto auction) {
        ModifyAuctionController.setAuctionId(auction.getId());
        SceneManager.clearCache("modifyauction.fxml");
        SceneManager.switchScene("modifyauction.fxml", false, false);
    }

    private void openAuctionDetail(AuctionDto auction) {
        AuctionDetailsController.setAuctionId(auction.getId());
        SceneManager.clearCache("auctiondetail.fxml");
        SceneManager.switchScene("auctiondetail.fxml", false, false);
    }

    private String resolveInitial(String value) {
        if (value == null || value.isBlank()) return "A";
        return value.substring(0, 1).toUpperCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

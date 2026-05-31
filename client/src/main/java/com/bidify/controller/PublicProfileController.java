package com.bidify.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.dto.PublicProfileDto;
import com.bidify.common.dto.PublicProfileStatsDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.model.ClientSession;
import com.bidify.service.PublicProfileClientService;
import com.bidify.media.ImageCache;
import com.bidify.navigation.MissionBarUtil;
import com.bidify.navigation.NavPage;
import com.bidify.ui.NotificationUtil;
import com.bidify.navigation.SceneManager;

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

public class PublicProfileController {
    private static String targetUsername;
    private final PublicProfileClientService profileService = new PublicProfileClientService();

    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private ImageView heroAvatarImageView;
    @FXML private Label heroAvatarLabel;
    @FXML private ImageView profileAvatarImageView;
    @FXML private Label profileAvatarIconLabel;
    @FXML private Label nicknameValueLabel;
    @FXML private Label usernameValueLabel;
    @FXML private Label emailValueLabel;
    @FXML private Label phoneNumberValueLabel;
    @FXML private Label roleValueLabel;
    @FXML private Label totalAuctionsLabel;
    @FXML private Label activeAuctionsLabel;
    @FXML private Label closedAuctionsLabel;
    @FXML private Label soldAuctionsLabel;
    @FXML private Label totalBidsLabel;
    @FXML private Label activeVolumeLabel;
    @FXML private Label sellRateLabel;
    @FXML private VBox auctionRowsContainer;
    @FXML private Label summaryLabel;
    @FXML private Button backButton;

    @FXML private Label reputationLabel;
    @FXML private Label reputationStarsLabel;
    @FXML private Label completionRateLabel;
    @FXML private Label completedSalesLabel;
    @FXML private Label failedSalesLabel;

    public static void setTargetUsername(String username) {
        targetUsername = username;
    }

    public static String getTargetUsername() {
        return targetUsername;
    }

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            MissionBarUtil.setup(NavPage.NONE, false, null);
            loadProfile();
        });
    }

    private void loadProfile() {
        String usernameToLoad = targetUsername;
        if (usernameToLoad == null || usernameToLoad.isBlank()) {
            usernameToLoad = ClientSession.getInstance().getCurrentUsername();
        }

        if (usernameToLoad == null || usernameToLoad.isBlank()) {
            NotificationUtil.error("No user specified and no session found.");
            SceneManager.goHome();
            return;
        }

        try {
            PublicProfileDto profile = profileService.getPublicProfile(usernameToLoad);
            renderProfile(profile);
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server.");
            SceneManager.goHome();
        } catch (Exception e) {
            NotificationUtil.error("Failed to load public profile: " + e.getMessage());
            SceneManager.goHome();
        }
    }

    private void renderProfile(PublicProfileDto profile) {
        if (profile == null) return;

        String displayName = DisplayUtil.defaultText(profile.getNickname(), profile.getUsername());
        pageTitleLabel.setText(displayName + "'s Profile");
        pageSubtitleLabel.setText("View " + displayName + "'s statistics, ratings, and auction activity history.");

        nicknameValueLabel.setText(displayName);
        usernameValueLabel.setText("@" + profile.getUsername());
        emailValueLabel.setText(DisplayUtil.defaultText(profile.getEmail(), "Not provided"));
        phoneNumberValueLabel.setText(DisplayUtil.defaultText(profile.getPhoneNumber(), "Not provided"));

        // Set avatar letter fallback
        String initial = resolveInitial(displayName);
        heroAvatarLabel.setText(initial);
        profileAvatarIconLabel.setText(initial);

        // Load avatar images if present
        String base64Image = profile.getProfileImageBase64();
        if (base64Image != null && !base64Image.isBlank()) {
            String cacheKey = "public_avatar_" + profile.getUsername() + "_" + base64Image.hashCode();
            Image img = ImageCache.getInstance().get(cacheKey, base64Image);
            if (img != null) {
                heroAvatarImageView.setImage(img);
                heroAvatarImageView.setVisible(true);
                heroAvatarLabel.setVisible(false);

                profileAvatarImageView.setImage(img);
                profileAvatarImageView.setVisible(true);
                profileAvatarIconLabel.setVisible(false);
            }
        } else {
            heroAvatarImageView.setVisible(false);
            heroAvatarLabel.setVisible(true);

            profileAvatarImageView.setVisible(false);
            profileAvatarIconLabel.setVisible(true);
        }

        // Render stats
        PublicProfileStatsDto stats = profile.getStats();
        if (stats != null) {
            totalAuctionsLabel.setText(String.valueOf(stats.getTotalAuctions()));
            activeAuctionsLabel.setText(String.valueOf(stats.getActiveAuctions()));
            closedAuctionsLabel.setText(String.valueOf(stats.getClosedAuctions()));
            soldAuctionsLabel.setText(String.valueOf(stats.getSoldAuctions()));
            totalBidsLabel.setText(String.valueOf(stats.getTotalBids()));
            activeVolumeLabel.setText(DisplayUtil.formatCashSuffix(stats.getActiveVolume()));
            sellRateLabel.setText(stats.getSellRate());
            reputationLabel.setText(DisplayUtil.defaultText(stats.getReputationLabel(), "New Seller"));
            reputationStarsLabel.setText(DisplayUtil.defaultText(stats.getStarVisual(), "★★★★★") + " (" + String.format("%.1f", stats.getStarRating()) + "/5.0)");
            completionRateLabel.setText(DisplayUtil.defaultText(stats.getCompletionRate(), "0.0%") + " completion");
            completedSalesLabel.setText(stats.getCompletedSales() + " completed sale" + (stats.getCompletedSales() == 1 ? "" : "s"));
            failedSalesLabel.setText(stats.getFailedSales() + " failed sale" + (stats.getFailedSales() == 1 ? "" : "s"));
        }

        // Render auctions list
        auctionRowsContainer.getChildren().clear();
        AuctionDto[] auctions = profile.getAuctions();
        if (auctions == null || auctions.length == 0) {
            auctionRowsContainer.getChildren().add(createEmptyState());
            summaryLabel.setText("No auctions created by this member.");
        } else {
            for (int i = 0; i < auctions.length; i++) {
                auctionRowsContainer.getChildren().add(createRow(auctions[i], i == auctions.length - 1));
            }
            summaryLabel.setText("Showing " + auctions.length + " auction" + (auctions.length == 1 ? "" : "s"));
        }
    }

    private Node createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(28, 32, 28, 32));

        Label title = new Label("No auctions found.");
        title.getStyleClass().add("empty-state-title");

        Label subtitle = new Label("This member hasn't created any auctions yet.");
        subtitle.getStyleClass().add("empty-state-subtitle");

        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private Node createRow(AuctionDto auction, boolean isLast) {
        GridPane row = new GridPane();
        row.getStyleClass().add(isLast ? "table-row-last" : "table-row");
        row.getColumnConstraints().addAll(
            createColumn(45),
            createColumn(20),
            createColumn(15),
            createColumn(20)
        );
        row.setPadding(new Insets(18, 32, 18, 32));
        row.setStyle("-fx-cursor: hand;");

        row.setOnMouseClicked(event -> {
            SceneManager.goAuctionDetail(auction.getId());
        });

        // 1. Title and details
        HBox details = new HBox(16);
        details.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().add(createThumbnailNode(auction));

        VBox textBox = new VBox(4);
        Label title = new Label(DisplayUtil.defaultText(auction.getAuctionName(), "Unnamed auction"));
        title.getStyleClass().add("item-title");

        List<String> metaParts = new ArrayList<>();
        if (auction.getCategory() != null && !auction.getCategory().isBlank()) {
            metaParts.add(auction.getCategory());
        }
        if (auction.getProductType() != null && !auction.getProductType().isBlank()) {
            metaParts.add(auction.getProductType());
        }
        String metaText = String.join(" • ", metaParts);
        Label meta = new Label(metaText);
        meta.getStyleClass().add("sku-text");

        textBox.getChildren().addAll(title, meta);
        details.getChildren().add(textBox);

        // 2. Pricing
        double priceValue = auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice();
        Label price = new Label(DisplayUtil.formatCashSuffix(priceValue));
        price.getStyleClass().add("item-title");

        // 3. Status Chip
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER_LEFT);
        Label statusChip = createStatusChip(auction.getStatus());
        statusBox.getChildren().add(statusChip);

        // 4. End time
        String timeStr = "";
        if (auction.getEndTime() != null) {
            try {
                java.time.LocalDateTime end = java.time.LocalDateTime.parse(auction.getEndTime());
                timeStr = end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (Exception e) {
                timeStr = auction.getEndTime();
            }
        }
        Label endTime = new Label(timeStr);
        endTime.getStyleClass().add("sku-text");
        endTime.setAlignment(Pos.CENTER_LEFT);

        row.add(details, 0, 0);
        row.add(price, 1, 0);
        row.add(statusBox, 2, 0);
        row.add(endTime, 3, 0);

        return row;
    }

    private ColumnConstraints createColumn(double percentWidth) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setPercentWidth(percentWidth);
        return constraints;
    }

    private Label createStatusChip(String statusStr) {
        String displayName = statusStr;
        String styleClass = "status-chip-generic";

        if (statusStr != null) {
            try {
                AuctionStatus status = AuctionStatus.valueOf(statusStr.toUpperCase());
                displayName = status.name().substring(0, 1).toUpperCase() + status.name().substring(1).toLowerCase();
                switch (status) {
                    case ACTIVE:
                        styleClass = "status-chip-active";
                        break;
                    case UPCOMING:
                        styleClass = "status-chip-upcoming";
                        break;
                    case COMPLETED:
                    case PAID:
                    case AWAITING_DELIVERY:
                        styleClass = "status-chip-completed";
                        break;
                    default:
                        styleClass = "status-chip-ended";
                        break;
                }
            } catch (IllegalArgumentException e) {
                // Ignore
            }
        }

        Label chip = new Label(displayName);
        chip.getStyleClass().addAll("status-chip", styleClass);
        return chip;
    }

    private Node createThumbnailNode(AuctionDto auction) {
        StackPane frame = new StackPane();
        frame.getStyleClass().add("thumb-frame");
        frame.setPrefSize(48, 48);

        Image image = decodeImage(auction.getThumbnailBase64());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(48);
            imageView.setFitHeight(48);
            imageView.setPreserveRatio(false);
            imageView.getStyleClass().add("thumb");
            frame.getChildren().add(imageView);
        } else {
            Label placeholder = new Label(resolveInitial(auction.getAuctionName()));
            placeholder.getStyleClass().add("thumb-placeholder");
            frame.getChildren().add(placeholder);
        }

        return frame;
    }

    private Image decodeImage(String base64) {
        if (base64 == null || base64.isBlank()) return null;
        try {
            Image img = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
            if (img.isError()) {
                return null;
            }
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void handleBack() {
        SceneManager.goHome();
    }

    private String resolveInitial(String value) {
        if (value == null || value.isBlank()) return "U";
        return value.substring(0, 1).toUpperCase();
    }
}

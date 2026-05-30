package com.bidify.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.utility.DisplayUtil;
import com.bidify.model.ClientSession;
import com.bidify.service.AuctionClientService;
import com.bidify.navigation.MissionBarUtil;
import com.bidify.navigation.NavPage;
import com.bidify.ui.NotificationUtil;
import com.bidify.navigation.SceneManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class SettlementsController {
    private final AuctionClientService auctionClientService = new AuctionClientService();
    private List<AuctionDto> allSettlements = List.of();

    @FXML
    private TextField searchField;

    @FXML
    private VBox settlementsContainer;

    @FXML
    private Label summaryLabel;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            MissionBarUtil.setup(NavPage.SETTLEMENTS, false, null);
            searchField.textProperty().addListener((obs, oldValue, newValue) -> renderSettlements(filterSettlements(newValue)));
            loadSettlements();
        });
    }

    @FXML
    private void handleRefresh() {
        loadSettlements();
    }

    private void loadSettlements() {
        try {
            AuctionDto[] settlements = auctionClientService.getUserSettlements();
            allSettlements = settlements != null ? List.of(settlements) : List.of();
            renderSettlements(filterSettlements(searchField.getText()));
        }
        catch (IOException e) {
            allSettlements = List.of();
            renderSettlements(List.of());
            NotificationUtil.error("Cannot connect to server.");
        }
        catch (Exception e) {
            allSettlements = List.of();
            renderSettlements(List.of());
            NotificationUtil.error("Failed to load settlements: " + e.getMessage());
        }
    }

    private List<AuctionDto> filterSettlements(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase();
        if (query.isBlank()) return allSettlements;

        String currentUsername = ClientSession.getInstance().getCurrentUsername();

        List<AuctionDto> filtered = new ArrayList<>();
        for (AuctionDto auction : allSettlements) {
            if (auction == null) continue;

            String role = "buyer";
            if (currentUsername != null && currentUsername.equalsIgnoreCase(auction.getSellerUsername())) {
                role = "seller";
            }

            String haystack = String.join(" ",
                safe(auction.getAuctionName()),
                safe(auction.getDescription()),
                safe(auction.getCategory()),
                safe(auction.getProductType()),
                safe(auction.getStatus()),
                role
            ).toLowerCase();

            if (haystack.contains(query))
                filtered.add(auction);
        }
        return filtered;
    }

    private void renderSettlements(List<AuctionDto> auctions) {
        settlementsContainer.getChildren().clear();

        if (auctions == null || auctions.isEmpty()) {
            settlementsContainer.getChildren().add(createEmptyState());
            summaryLabel.setText(allSettlements.isEmpty() ? "No pending settlements" : "No settlements match your search");
            return;
        }

        for (int i = 0; i < auctions.size(); i++) {
            settlementsContainer.getChildren().add(createRow(auctions.get(i), i == auctions.size() - 1));
        }

        summaryLabel.setText("Showing " + auctions.size() + " settlement" + (auctions.size() == 1 ? "" : "s"));
    }

    private Node createEmptyState() {
        VBox box = new VBox(10);
        box.getStyleClass().add("empty-state");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(28, 32, 28, 32));

        Label title = new Label(allSettlements.isEmpty() ? "Your settlements list is empty." : "No settlements match that search.");
        title.getStyleClass().add("empty-state-title");

        Label subtitle = new Label(allSettlements.isEmpty()
            ? "Ended auctions requiring actions (payment or shipping) will be shown here."
            : "Try a different keyword or clear the search field.");
        subtitle.getStyleClass().add("empty-state-subtitle");
        subtitle.setWrapText(true);

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
        row.setPadding(new Insets(24, 32, 24, 32));
        row.setStyle("-fx-cursor: hand;");

        // Click để chuyển đến trang chi tiết phiên đấu giá
        row.setOnMouseClicked(event -> {
            SceneManager.goAuctionDetail(auction.getId());
        });

        // 1. Chi tiết sản phẩm (Hộp chứa ảnh thu nhỏ và Tên/Mô tả)
        HBox details = new HBox(16);
        details.setAlignment(Pos.CENTER_LEFT);
        details.getChildren().add(createThumbnailNode(auction));

        VBox textBox = new VBox(6);
        Label title = new Label(DisplayUtil.defaultText(auction.getAuctionName(), "Unnamed auction"));
        title.getStyleClass().add("item-title");

        Label meta = new Label(buildMetaLine(auction));
        meta.getStyleClass().add("sku-text");
        textBox.getChildren().addAll(title, meta);
        details.getChildren().add(textBox);

        // 2. Giá trị đấu giá thắng cuối cùng
        Label bidValue = new Label(DisplayUtil.formatCashSuffix(auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice()));
        bidValue.getStyleClass().add("item-title");
        bidValue.setAlignment(Pos.CENTER_LEFT);

        // 3. Vai trò của người dùng (Người mua hay Người bán)
        String currentUsername = ClientSession.getInstance().getCurrentUsername();
        boolean isSeller = currentUsername != null && currentUsername.equalsIgnoreCase(auction.getSellerUsername());
        
        Label roleChip = new Label(isSeller ? "SELLER" : "BUYER");
        roleChip.getStyleClass().addAll("role-chip", isSeller ? "role-chip-seller" : "role-chip-buyer");

        // 4. Trạng thái quyết toán
        HBox statusBox = new HBox();
        statusBox.setAlignment(Pos.CENTER);
        
        Label statusChip = createStatusChip(auction.getStatus());
        statusBox.getChildren().add(statusChip);

        // Thêm các thành phần vào GridPane
        row.add(details, 0, 0);
        row.add(bidValue, 1, 0);
        row.add(roleChip, 2, 0);
        row.add(statusBox, 3, 0);

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
                switch (status) {
                    case AWAITING_PAYMENT:
                        displayName = "Awaiting Payment";
                        styleClass = "status-chip-awaiting-payment";
                        break;
                    case AWAITING_DELIVERY:
                        displayName = "Awaiting Delivery";
                        styleClass = "status-chip-awaiting-delivery";
                        break;
                    case COMPLETED:
                        displayName = "Completed";
                        styleClass = "status-chip-completed";
                        break;
                    case CANCELED:
                        displayName = "Canceled";
                        styleClass = "status-chip-canceled";
                        break;
                    default:
                        displayName = status.name();
                        styleClass = "status-chip-generic";
                        break;
                }
            } catch (IllegalArgumentException e) {
                // Trạng thái không khớp enum
            }
        }

        Label chip = new Label(displayName);
        chip.getStyleClass().addAll("status-chip", styleClass);
        return chip;
    }

    private Node createThumbnailNode(AuctionDto auction) {
        StackPane frame = new StackPane();
        frame.getStyleClass().add("thumb-frame");
        frame.setPrefSize(64, 64);

        Image image = decodeImage(auction.getThumbnailBase64());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(64);
            imageView.setFitHeight(64);
            imageView.setPreserveRatio(false);
            imageView.getStyleClass().add("thumb");
            frame.getChildren().add(imageView);
        }
        else {
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
        }
        catch (Exception e) {
            return null;
        }
    }

    private String buildMetaLine(AuctionDto auction) {
        List<String> parts = new ArrayList<>();
        if (!safe(auction.getCategory()).isBlank())
            parts.add(auction.getCategory());
        if (!safe(auction.getProductType()).isBlank())
            parts.add(auction.getProductType());
        
        parts.add("Seller: " + DisplayUtil.defaultText(auction.getSellerUsername(), "Unknown"));
        return String.join(" • ", parts);
    }

    private String resolveInitial(String value) {
        if (value == null || value.isBlank()) return "A";
        return value.substring(0, 1).toUpperCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

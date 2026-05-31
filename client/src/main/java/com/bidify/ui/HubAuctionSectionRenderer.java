package com.bidify.ui;

import java.io.IOException;
import java.util.Map;

import com.bidify.common.dto.AuctionDto;
import com.bidify.controller.AuctionCardController;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

public class HubAuctionSectionRenderer {
    public static void renderSection(
            HBox container,
            Label emptyLabel,
            AuctionDto[] auctions,
            Map<String, AuctionCardController> controllerMap,
            String emptyMessage,
            Runnable resetNav,
            Runnable refreshNav) {
        cleanupControllers(controllerMap);
        container.getChildren().clear();
        if (resetNav != null) {
            resetNav.run();
        }

        if (auctions == null || auctions.length == 0) {
            emptyLabel.setText(emptyMessage);
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            if (refreshNav != null) {
                refreshNav.run();
            }
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        for (AuctionDto auction : auctions) {
            container.getChildren().add(loadAuctionCard(auction, controllerMap));
        }

        if (refreshNav != null) {
            refreshNav.run();
        }
    }

    private static AnchorPane loadAuctionCard(AuctionDto auction, Map<String, AuctionCardController> controllerMap) {
        try {
            FXMLLoader loader = new FXMLLoader(HubAuctionSectionRenderer.class.getResource("/fxml/auction-card.fxml"));
            AnchorPane card = loader.load();
            AuctionCardController controller = loader.getController();
            controller.bind(auction);
            if (auction != null && auction.getId() != null) {
                controllerMap.put(auction.getId(), controller);
            }
            return card;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load auction-card.fxml", e);
        }
    }

    public static void cleanupControllers(Map<String, AuctionCardController> controllerMap) {
        if (controllerMap == null) return;
        for (AuctionCardController controller : controllerMap.values()) {
            if (controller != null) {
                controller.cleanup();
            }
        }
        controllerMap.clear();
    }
}

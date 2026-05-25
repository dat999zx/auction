package com.bidify.utility;

import com.bidify.controller.MissionBarController;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

final class SceneShell {
    private static final double MISSION_BAR_HEIGHT = 78.0;

    private final StackPane shell = new StackPane();
    private final StackPane contentLayer = new StackPane();
    private final StackPane overlayLayer = new StackPane();
    private Parent missionBarRoot;
    private MissionBarController missionBarController;
    private boolean showMissionBar;

    StackPane root() {
        return shell;
    }

    StackPane contentLayer() {
        return contentLayer;
    }

    StackPane overlayLayer() {
        return overlayLayer;
    }

    MissionBarController missionBarController() {
        return missionBarController;
    }

    void initialize(LoadingOverlay loadingOverlay) {
        overlayLayer.getChildren().setAll(loadingOverlay.node());
        overlayLayer.setAlignment(Pos.TOP_CENTER);
        overlayLayer.setStyle("-fx-background-color: rgba(0, 0, 0, 0);");
        overlayLayer.setVisible(true);
        overlayLayer.setManaged(true);
        overlayLayer.setMouseTransparent(true);

        contentLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        overlayLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        if (missionBarRoot == null) {
            try {
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/includes/mission-bar.fxml"));
                missionBarRoot = loader.load();
                missionBarController = loader.getController();
                ButtonHoverUtil.apply(missionBarRoot);
            } catch (Exception e) {
                throw new IllegalStateException("FXML not found: /fxml/includes/mission-bar.fxml", e);
            }
        }

        updateLayout();
    }

    void setContent(Parent root, boolean showMissionBar) {
        this.showMissionBar = showMissionBar;
        contentLayer.getChildren().setAll(root);
        updateLayout();
    }

    void updateLayout() {
        shell.getChildren().clear();
        shell.getChildren().add(contentLayer);
        
        if (missionBarRoot != null) {
            missionBarRoot.setVisible(showMissionBar);
            missionBarRoot.setManaged(showMissionBar);
            StackPane.setMargin(contentLayer, new Insets(showMissionBar ? MISSION_BAR_HEIGHT : 0.0, 0.0, 0.0, 0.0));
            shell.getChildren().add(missionBarRoot);
        }
        
        shell.getChildren().add(overlayLayer);
    }
}

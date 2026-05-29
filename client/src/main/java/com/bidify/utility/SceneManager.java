package com.bidify.utility;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.bidify.controller.MissionBarController;

import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SceneManager {
    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);
    private static final long LOADING_DELAY_MS = 250;
    private static final String LOGIN_SCENE = "login.fxml";
    private static final String REGISTER_SCENE = "register.fxml";

    private static Stage stage;
    private static final SceneCache sceneCache = new SceneCache();
    private static final LoadingOverlay loadingOverlay = new LoadingOverlay();
    private static final SceneShell sceneShell = new SceneShell();

    private static final AtomicLong navigationToken = new AtomicLong();
    private static final AtomicBoolean isSwitchingScene = new AtomicBoolean(false);

    private SceneManager() {}

    public static void setStage(Stage s) {
        if (stage != null) return;
        stage = s;
        loadingOverlay.initialize();
        sceneShell.initialize(loadingOverlay);
    }

    public static void switchScene(String fxml) {
        switchScene(fxml, true, false);
    }

    public static void switchScene(String fxml, boolean remember) {
        switchScene(fxml, remember, false);
    }

    public static void switchScene(String fxml, boolean remember, boolean showBar) {
        if (!isSwitchingScene.compareAndSet(false, true)) return;
        if (!remember) sceneCache.clear(fxml);
        Platform.runLater(() -> setInputBlocked(true));

        long token = navigationToken.incrementAndGet();
        AtomicBoolean completed = new AtomicBoolean(false);

        Thread loadingDelayThread = new Thread(() -> {
            try {
                Thread.sleep(LOADING_DELAY_MS);
                if (!completed.get() && token == navigationToken.get()) {
                    Platform.runLater(() -> {
                        if (!completed.get() && token == navigationToken.get()) showLoading(true);
                    });
                }
            } catch (InterruptedException e) {
                logger.warn("Exception occurred", e);
                Thread.currentThread().interrupt();
            }
        });
        loadingDelayThread.setDaemon(true);
        loadingDelayThread.start();

        Thread loaderThread = new Thread(() -> {
            try {
                Parent root = sceneCache.load(fxml, remember);

                Platform.runLater(() -> {
                    if (token != navigationToken.get()) return;
                    completed.set(true);
                    ButtonHoverUtil.apply(root);

                    Scene scene = stage.getScene();
                    if (scene == null) {
                        sceneShell.setContent(root, showBar);
                        stage.setScene(new Scene(sceneShell.root()));
                        scene = stage.getScene();
                    } else {
                        if (scene.getRoot() != sceneShell.root()) {
                            Parent currentRoot = scene.getRoot();
                            sceneShell.contentLayer().getChildren().setAll(currentRoot);
                            scene.setRoot(sceneShell.root());
                        }
                        sceneShell.setContent(root, showBar);
                    }

                    loadCss(scene, fxml);
                    showLoading(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (token != navigationToken.get()) return;
                    completed.set(true);
                    showLoading(false);
                    logger.warn("Exception occurred", e);
                });
            }
        });
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private static void setInputBlocked(boolean blocked) {
        if (stage == null || stage.getScene() == null) return;
        stage.getScene().getRoot().setMouseTransparent(blocked);
    }

    private static void finishSwitchScene() {
        Platform.runLater(() -> setInputBlocked(false));
        isSwitchingScene.set(false);
    }

    public static void preloadScenes(String... fxmls) {
        if (fxmls == null) return;

        for (String fxml : fxmls) {
            if (fxml == null || fxml.isBlank() || sceneCache.contains(fxml)) continue;

            Thread preloadThread = new Thread(() -> {
                try {
                    sceneCache.load(fxml, true);
                } catch (Exception e) {
                    logger.error("Exception occurred", e);
                }
            });
            preloadThread.setDaemon(true);
            preloadThread.start();
        }
    }

    public static void clearCache(String fxml) {
        sceneCache.clear(fxml);
    }

    public static void clearAllCache() {
        sceneCache.clearAll();
    }

    public static void preloadAuthScenes() {
        preloadScenes(LOGIN_SCENE, REGISTER_SCENE);
    }

    public static void resetMissionBar() {
        Runnable reset = () -> {
            MissionBarController controller = sceneShell.missionBarController();
            if (controller != null) controller.resetState();
        };
        if (Platform.isFxApplicationThread()) reset.run();
        else Platform.runLater(reset);
    }

    public static MissionBarController getMissionBarController() {
        return sceneShell.missionBarController();
    }

    public static StackPane getOverlayLayer() {
        return sceneShell.overlayLayer();
    }

    private static void loadCss(Scene scene, String fxml) {
        String cssName = fxml.replace(".fxml", ".css");
        var cssUrl = SceneManager.class.getResource("/css/" + cssName);

        scene.getStylesheets().clear();
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
    }

    private static void showLoading(boolean visible) {
        if (stage == null) return;
        if (visible) {
            loadingOverlay.show();
        } else {
            loadingOverlay.hide(SceneManager::finishSwitchScene);
        }
    }
}

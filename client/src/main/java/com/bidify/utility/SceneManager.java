package com.bidify.utility;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.bidify.controller.MissionBarController;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;
import javafx.util.Duration;

/*
chuyển đổi sang các scene khác dễ dàng hơn
SceneManager.switchScene(fxml) để đổi sang scene mới và tự ghi nhớ
SceneManager.switchScene(fxml, false) để đổi sang scene mới mà không ghi nhớ lại
mục đích của việc ghi nhớ là để lần sau nếu có mở lại scene đó thì load nhanh hơn
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SceneManager {
    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);
    private static final long LOADING_DELAY_MS = 250;
    private static final double MISSION_BAR_HEIGHT = 78.0;

    private static Stage stage;
    private static final Map<String, Parent> cache = new ConcurrentHashMap<>();

    private static final StackPane shell = new StackPane();
    private static final StackPane contentLayer = new StackPane();
    private static final StackPane overlayLayer = new StackPane();
    private static final StackPane loadingNode = new StackPane();
    private static Parent missionBarRoot;
    private static MissionBarController missionBarController;
    private static boolean showMissionBar;

    private static final AtomicLong navigationToken = new AtomicLong();
    private static final AtomicBoolean isSwitchingScene = new AtomicBoolean(false);
    private static RotateTransition spinAnimation;

    private SceneManager() {}

    public static void setStage(Stage s) {
        if (stage != null) return;
        stage = s;
        initOverlay();
        initShell();
    }

    public static void switchScene(String fxml) {
        switchScene(fxml, true, false);
    }

    public static void switchScene(String fxml, boolean remember) {
        switchScene(fxml, remember, false);
    }

    public static void switchScene(String fxml, boolean remember, boolean showBar) {

        if (!isSwitchingScene.compareAndSet(false, true)) return;
        Platform.runLater(() -> setInputBlocked(true));
        showMissionBar = showBar;

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
            }
            catch (InterruptedException e) {
                logger.warn("Exception occurred", e);
                Thread.currentThread().interrupt();
            }
        });
        loadingDelayThread.setDaemon(true);
        loadingDelayThread.start();

        Thread loaderThread = new Thread(() -> {
            try {
                Parent root = loadFxml(fxml, remember);

                Platform.runLater(() -> {
                    if (token != navigationToken.get()) return;
                    completed.set(true);

                    Scene scene = stage.getScene();
                    if (scene == null) {
                        contentLayer.getChildren().setAll(root);
                        updateShellLayout();
                        stage.setScene(new Scene(shell));
                        scene = stage.getScene();
                    }
                    else {
                        if (scene.getRoot() != shell) {
                            Parent currentRoot = scene.getRoot();
                            contentLayer.getChildren().setAll(currentRoot);
                            scene.setRoot(shell);
                        }
                        contentLayer.getChildren().setAll(root);
                        updateShellLayout();
                    }

                    loadCss(scene, fxml);
                    showLoading(false);
                });
            }
            catch (Exception e) {
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
            if (fxml == null || fxml.isBlank() || cache.containsKey(fxml)) continue;

            Thread preloadThread = new Thread(() -> {
                try {
                    loadFxml(fxml, true);
                }
                catch (Exception e) {
                    logger.error("Exception occurred", e);
                }
            });
            preloadThread.setDaemon(true);
            preloadThread.start();
        }
    }

    public static void clearCache(String fxml) {
        if (fxml == null) return;
        cache.remove(fxml);
    }

    public static void clearAllCache() {
        cache.clear();
    }

    public static MissionBarController getMissionBarController() {
        return missionBarController;
    }

    public static StackPane getOverlayLayer() {
        return overlayLayer;
    }

    private static Parent loadFxml(String fxml, boolean remember) throws Exception {
        Parent cached = cache.get(fxml);
        if (remember && cached != null) return cached;

        Parent root = doLoadFxml(fxml);
        if (remember) cache.put(fxml, root);
        return root;
    }

    private static Parent doLoadFxml(String fxml) throws Exception {
        var location = SceneManager.class.getResource("/fxml/" + fxml);
        if (location == null) throw new IllegalArgumentException("FXML not found: /fxml/" + fxml);

        FXMLLoader loader = new FXMLLoader(location);
        return loader.load();
    }

    private static void loadCss(Scene scene, String fxml) {
        String cssName = fxml.replace(".fxml", ".css");
        var cssUrl = SceneManager.class.getResource("/css/" + cssName);

        scene.getStylesheets().clear();
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
    }

    private static void initOverlay() {
        Circle baseRing = new Circle(12);
        baseRing.setFill(Color.TRANSPARENT);
        baseRing.setStroke(Color.web("#000666", 0.14));
        baseRing.setStrokeWidth(2);

        Arc spinner = new Arc(0, 0, 12, 12, 18, 230);
        spinner.setType(ArcType.OPEN);
        spinner.setFill(Color.TRANSPARENT);
        spinner.setStroke(new LinearGradient(
            0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#000666")),
            new Stop(1, Color.web("#0048d8"))
        ));
        spinner.setStrokeWidth(2.5);
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        spinner.setEffect(new DropShadow(7, Color.web("#0048d8", 0.26)));

        loadingNode.getChildren().setAll(baseRing, spinner);
        loadingNode.setOpacity(0);
        loadingNode.setTranslateY(-450);
        loadingNode.setVisible(false);
        loadingNode.setManaged(false);
        StackPane.setAlignment(loadingNode, Pos.TOP_CENTER);
        StackPane.setMargin(loadingNode, new Insets(10, 0, 0, 0));

        overlayLayer.getChildren().setAll(loadingNode);
        overlayLayer.setAlignment(Pos.TOP_CENTER);
        overlayLayer.setStyle("-fx-background-color: rgba(0, 0, 0, 0);");
        overlayLayer.setVisible(true);
        overlayLayer.setManaged(true);
        overlayLayer.setMouseTransparent(true);

        contentLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        overlayLayer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        spinAnimation = new RotateTransition(Duration.millis(850), loadingNode);
        spinAnimation.setByAngle(360);
        spinAnimation.setCycleCount(RotateTransition.INDEFINITE);
        spinAnimation.setInterpolator(Interpolator.LINEAR);
    }

    private static void initShell() {
        if (missionBarRoot == null) {
            try {
                FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource("/fxml/includes/mission-bar.fxml"));
                missionBarRoot = loader.load();
                missionBarController = loader.getController();
            } catch (Exception e) {
                throw new IllegalStateException("FXML not found: /fxml/includes/mission-bar.fxml", e);
            }
        }

        updateShellLayout();
    }

    private static void updateShellLayout() {
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

    private static void showLoading(boolean visible) {
        if (stage == null) return;
        if (visible) {
            animateShowLoading();
        } else {
            animateHideLoading();
        }
    }

    private static void animateShowLoading() {
        loadingNode.setVisible(true);
        loadingNode.setManaged(true);

        spinAnimation.playFromStart();

        FadeTransition fade = new FadeTransition(Duration.millis(100), loadingNode);
        fade.setFromValue(loadingNode.getOpacity());
        fade.setToValue(1);

        TranslateTransition drop = new TranslateTransition(Duration.millis(150), loadingNode);
        drop.setFromY(loadingNode.getTranslateY());
        drop.setToY(-350);
        drop.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, drop).play();
    }

    private static void animateHideLoading() {
        FadeTransition fade = new FadeTransition(Duration.millis(50), loadingNode);
        fade.setFromValue(loadingNode.getOpacity());
        fade.setToValue(0);

        TranslateTransition rise = new TranslateTransition(Duration.millis(120), loadingNode);
        rise.setFromY(loadingNode.getTranslateY());
        rise.setToY(-450);
        rise.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition hide = new ParallelTransition(fade, rise);
        hide.setOnFinished(event -> {
            spinAnimation.stop();
            loadingNode.setVisible(false);
            loadingNode.setManaged(false);
            loadingNode.setOpacity(0);
            loadingNode.setTranslateY(-450);
            finishSwitchScene();
        });
        hide.play();
    }
}

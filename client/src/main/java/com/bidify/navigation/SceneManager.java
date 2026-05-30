package com.bidify.navigation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.bidify.controller.AuctionDetailsController;
import com.bidify.controller.InventoryController;
import com.bidify.controller.ItemDetailController;
import com.bidify.controller.MissionBarController;
import com.bidify.controller.ModifyAuctionController;
import com.bidify.controller.PublicProfileController;
import com.bidify.ui.ButtonHoverUtil;

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
    private static Parent currentRoot;
    private static Object currentController;

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
                Object controller = sceneCache.getController(root);

                Platform.runLater(() -> {
                    if (token != navigationToken.get()) return;
                    completed.set(true);
                    cleanupCurrentController(controller);
                    sceneCache.forgetUncached(currentRoot);
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
                    currentRoot = root;
                    currentController = controller;
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

    public static void goHome() {
        switchScene("hub.fxml", false, true);
    }

    public static void goCreateAuction() {
        switchScene("create-auction.fxml", false, true);
    }

    public static void goInventory() {
        switchScene("inventory.fxml", false, true);
    }

    public static void goMyInventory() {
        InventoryController.setManagedOwnerUsername(null);
        switchScene("inventory.fxml", false, true);
    }

    public static void goManagedInventory(String ownerUsername) {
        InventoryController.setManagedOwnerUsername(ownerUsername);
        switchScene("inventory.fxml", false, true);
    }

    public static void goItemEditor(String itemId) {
        ItemDetailController.setItemId(itemId);
        switchScene("item-detail.fxml", false, false);
    }

    public static void goAuctionDetail(String auctionId) {
        goAuctionDetail(auctionId, false);
    }

    public static void goAuctionDetail(String auctionId, boolean showBar) {
        AuctionDetailsController.setAuctionId(auctionId);
        switchScene("auctiondetail.fxml", false, showBar);
    }

    public static void goModifyAuction(String auctionId) {
        ModifyAuctionController.setAuctionId(auctionId);
        switchScene("modifyauction.fxml", false, false);
    }

    public static void goPublicProfile(String username) {
        PublicProfileController.setTargetUsername(username);
        switchScene("public-profile.fxml", false, true);
    }

    public static void goUserProfile() {
        switchScene("user-profile.fxml", false, true);
    }

    public static void goWallet() {
        switchScene("wallet.fxml", false, true);
    }

    public static void goHistory() {
        switchScene("history.fxml", false, true);
    }

    public static void goSettlements() {
        switchScene("settlements.fxml", false, true);
    }

    public static void goAdminUsers() {
        switchScene("admin-users.fxml", false, true);
    }

    public static void goAdminWalletRequests() {
        switchScene("admin-wallet-requests.fxml", false, true);
    }

    public static void goAdminAuctions() {
        switchScene("admin-auctions.fxml", false, true);
    }

    public static void goMyAuctions() {
        switchScene("myauctions.fxml", false, true);
    }

    public static void goLoginAfterLogout() {
        clearAllCache();
        resetMissionBar();
        switchScene(LOGIN_SCENE, true, false);
        preloadScenes(REGISTER_SCENE);
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

    private static void cleanupCurrentController(Object nextController) {
        if (currentController == null || currentController == nextController)
            return;

        if (currentController instanceof CleanableController cleanableController) {
            try {
                cleanableController.cleanup();
            }
            catch (Exception e) {
                logger.warn("Controller cleanup failed", e);
            }
        }
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

package com.bidify;

import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;
import com.bidify.event.EventManager;
import com.bidify.ui.NotificationUtil;
import com.bidify.media.SoundUtil;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.net.URL;

import com.bidify.network.SocketClient;
import com.bidify.navigation.SceneManager;
import com.bidify.model.ClientSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int DEFAULT_SERVER_PORT = 5000;
    private static final String APP_ICON_RESOURCE = "/images/bidify-logo-sidebar.png";
    private static final double WINDOW_ASPECT_RATIO = 16.0 / 9.0;

    private boolean adjustingWindowAspectRatio;

    @Override
    public void start(Stage stage) throws Exception {
        logger.info("Connecting to server...");
        SocketClient client = SocketClient.getClient();
        client.connect(resolveServerHost(), resolveServerPort());
        
        stage.setTitle("Bidify");
        setAppIcon(stage);
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setResizable(true);
        stage.setMaximized(true);

        stage.widthProperty().addListener((obs, oldVal, newVal) -> adjustWindowHeight(stage, newVal.doubleValue()));
        stage.heightProperty().addListener((obs, oldVal, newVal) -> adjustWindowWidth(stage, newVal.doubleValue()));

        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11)
                stage.setFullScreen(!stage.isFullScreen());
        });

        SceneManager.setStage(stage);
        registerGlobalEventHandlers();

        SceneManager.switchScene("login.fxml", true, false);
        SceneManager.preloadScenes("register.fxml");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        SocketClient.getClient().close();
    }

    public static void main(String[] args) {
        launch();
    }

    private static String resolveServerHost() {
        String host = System.getProperty("server.host");
        if (host == null || host.isBlank())
            return DEFAULT_SERVER_HOST;
        return host.trim();
    }

    private static int resolveServerPort() {
        String rawPort = System.getProperty("server.port");
        if (rawPort == null || rawPort.isBlank())
            return DEFAULT_SERVER_PORT;

        try {
            return Integer.parseInt(rawPort.trim());
        }
        catch (NumberFormatException e) {
            logger.warn("Invalid server.port '{}', using default {}", rawPort, DEFAULT_SERVER_PORT);
            return DEFAULT_SERVER_PORT;
        }
    }

    private void adjustWindowHeight(Stage stage, double width) {
        if (shouldSkipAspectRatioAdjustment(stage, width)) return;

        adjustingWindowAspectRatio = true;
        try {
            stage.setHeight(calculateHeightForWidth(width));
        }
        finally {
            adjustingWindowAspectRatio = false;
        }
    }

    private void adjustWindowWidth(Stage stage, double height) {
        if (shouldSkipAspectRatioAdjustment(stage, height)) return;

        adjustingWindowAspectRatio = true;
        try {
            stage.setWidth(calculateWidthForHeight(height));
        }
        finally {
            adjustingWindowAspectRatio = false;
        }
    }

    private boolean shouldSkipAspectRatioAdjustment(Stage stage, double value) {
        return adjustingWindowAspectRatio
                || stage.isFullScreen()
                || stage.isMaximized()
                || !Double.isFinite(value)
                || value <= 0.0;
    }

    static double calculateHeightForWidth(double width) {
        return width / WINDOW_ASPECT_RATIO;
    }

    static double calculateWidthForHeight(double height) {
        return height * WINDOW_ASPECT_RATIO;
    }

    private static void setAppIcon(Stage stage) {
        URL iconResource = MainApp.class.getResource(APP_ICON_RESOURCE);
        if (iconResource == null) {
            logger.warn("App icon resource not found: {}", APP_ICON_RESOURCE);
            return;
        }

        stage.getIcons().add(new Image(iconResource.toExternalForm()));
    }

    private static void registerGlobalEventHandlers() {
        EventManager.getInstance().subscribe(EventType.FORCED_LOGOUT, MainApp::handleForcedLogout);
    }

    private static void handleForcedLogout(Event event) {
        SoundUtil.error();
        ClientSession.getInstance().clear();
        if (event != null && event.getMessage() != null && !event.getMessage().isBlank())
            NotificationUtil.info(event.getMessage());
        SceneManager.goLoginAfterLogout();
    }
}

package com.bidify;

import javafx.application.Application;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import com.bidify.network.SocketClient;
import com.bidify.utility.SceneManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);
    private static final String DEFAULT_SERVER_HOST = "localhost";
    private static final int DEFAULT_SERVER_PORT = 5000;

    @Override
    public void start(Stage stage) throws Exception {
        logger.info("Connecting to server...");
        SocketClient client = SocketClient.getClient();
        client.connect(resolveServerHost(), resolveServerPort());
        
        stage.setTitle("Bidify");
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setResizable(true);

        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isFullScreen() && !Double.isNaN(newVal.doubleValue()))
                stage.setHeight(newVal.doubleValue() * 9.0 / 16.0);
        });

        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11)
                stage.setFullScreen(!stage.isFullScreen());
        });

        SceneManager.setStage(stage);

        SceneManager.preloadScenes("login.fxml", "register.fxml");

        SceneManager.switchScene("login.fxml");
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
}

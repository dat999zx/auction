package com.bidify;

import javafx.application.Application;
import javafx.stage.Stage;

import com.bidify.network.SocketClient;
import com.bidify.utility.SceneManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage stage) throws Exception {
        logger.info("Connecting to server...");
        SocketClient client = SocketClient.getClient();
        client.connect("localhost", 5000);
        
        stage.setTitle("Bidify");
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setWidth(1280);
        stage.setHeight(720);
        stage.setResizable(true);

        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isFullScreen() && !Double.isNaN(newVal.doubleValue())) {
                stage.setHeight(newVal.doubleValue() * 9.0 / 16.0);
            }
        });

        stage.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            }
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
}

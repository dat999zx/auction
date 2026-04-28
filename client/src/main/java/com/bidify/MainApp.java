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
        stage.setMaxWidth(1280);
        stage.setMinHeight(800);
        stage.setMaxHeight(800);
        SceneManager.setStage(stage);

        // Only preload anonymous screens. Authenticated screens run network calls in initialize().
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

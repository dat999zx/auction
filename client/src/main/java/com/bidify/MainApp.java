package com.bidify;

import javafx.application.Application;
import javafx.stage.Stage;

import com.bidify.network.SocketClient;
import com.bidify.utility.SceneManager;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("Connecting to server...");
        SocketClient client = SocketClient.getClient();
        client.connect("localhost", 5000);
        
        stage.setTitle("Bidify");
        stage.setMinWidth(1280);
        stage.setMaxWidth(1280);
        stage.setMinHeight(800);
        stage.setMaxHeight(800);
        SceneManager.setStage(stage);

        SceneManager.preloadScenes("login.fxml", "register.fxml", "hub.fxml", "user-profile.fxml");

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

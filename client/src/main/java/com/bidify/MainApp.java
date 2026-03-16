package com.bidify;

import javafx.application.Application;
import javafx.stage.Stage;

import com.bidify.utility.SceneManager;
import com.bidify.utility.SocketClient;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        SocketClient client = SocketClient.getClient();
        client.connect("localhost", 5000);
        
        SceneManager.setStage(stage);
        stage.setTitle("Bidify");
        stage.setMinWidth(1280);
        stage.setMaxWidth(1280);
        stage.setMinHeight(800);
        stage.setMaxHeight(800);
        SceneManager.switchScene("hub.fxml");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
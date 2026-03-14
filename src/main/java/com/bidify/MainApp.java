package com.bidify;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import com.bidify.utility.SceneManager;;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        SceneManager.setStage(stage);
        stage.setTitle("Bidify");
        stage.setMinWidth(1280);
        stage.setMaxWidth(1280);
        stage.setMinHeight(800);
        stage.setMaxHeight(800);
        SceneManager.switchScene("login.fxml");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
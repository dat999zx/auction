package com.bidify;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 800);
        scene.getStylesheets().add(MainApp.class.getResource("/css/login.css").toExternalForm());
        stage.setTitle("Bidify");
        stage.setMinWidth(1280);
        stage.setMaxWidth(1280);
        stage.setMinHeight(800);
        stage.setMaxHeight(800);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
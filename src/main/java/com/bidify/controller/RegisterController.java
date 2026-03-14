package com.bidify.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.PasswordField;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;

import com.bidify.MainApp;
import com.bidify.exception.ValidationException;

public class RegisterController {
    @FXML
    private TextField name;
    @FXML
    private TextField email;
    @FXML
    private PasswordField password;
    @FXML
    private PasswordField passwordConfirm;
    @FXML
    private Label messageLabel;
    
    // nút register
    @FXML
    private void handleRegister(){
        try{
            
        }
        catch(ValidationException e){
            
        }
    }

    // đổi sang scene đăng nhập
    @FXML
    private void toLogin(ActionEvent event){
        try{
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(MainApp.class.getResource("/css/login.css").toExternalForm());
            stage.setScene(scene);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}

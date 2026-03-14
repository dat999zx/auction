package com.bidify.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;

import com.bidify.exception.ValidationException;
import com.bidify.utility.SceneManager;

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
    private void toLogin(ActionEvent event){ SceneManager.switchScene("login.fxml"); }
}

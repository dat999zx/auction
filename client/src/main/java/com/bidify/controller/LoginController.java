package com.bidify.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;

import com.bidify.utility.SceneManager;

import com.bidify.common.exception.ValidationException;

public class LoginController {
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;
    
    // nút login
    @FXML
    private void handleLogin(){
        try{
            String email = emailField.getText();
            String password = passwordField.getText();
            
            if (email.isEmpty()) throw new ValidationException("Email must not be empty");
            if (!email.contains("@") || email.contains(" ")) throw new ValidationException("Invalid email");
            if (password.contains(" ")) throw new ValidationException("Invalid password");
            if (password.length() < 6) throw new ValidationException("Password is too short");
            
            System.out.println(email);
            System.out.println(password);
        }
        catch(ValidationException e){
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
            System.out.println("Validation error: " + e.getMessage());
        }
    }

    // đổi sang scene đăng kí
    @FXML
    private void toRegister(ActionEvent event){ SceneManager.switchScene("register.fxml"); }
}

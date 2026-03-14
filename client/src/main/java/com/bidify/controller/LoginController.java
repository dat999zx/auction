package com.bidify.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;

import com.bidify.utility.SceneManager;
import com.bidify.exception.ValidationException;

public class LoginController {
    @FXML
    private TextField email;
    @FXML
    private PasswordField password;
    @FXML
    private Label messageLabel;
    
    // nút login
    @FXML
    private void handleLogin(){
        try{
            String userEmail = email.getText().trim();
            String userPassword = password.getText();
            
            if (userEmail.isEmpty()) throw new ValidationException("Email must not be empty");
            if (!userEmail.contains("@")) throw new ValidationException("Invalid email");
            if (userPassword.length() < 6) throw new ValidationException("Password is too short");
            
            System.out.println(userEmail);
            System.out.println(userPassword);
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

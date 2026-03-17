package com.bidify.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;

import com.bidify.utility.SceneManager;
import com.bidify.utility.SocketClient;

import java.io.IOException;

import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.LoginRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.util.ValidationUtil;
import com.bidify.common.exception.AuthException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label messageLabel;
    
    // nút login
    @FXML
    private void handleLogin(){
        try{
            String username = usernameField.getText();
            String password = passwordField.getText();

            ValidationUtil.validateUsername(username);
            ValidationUtil.validatePassword(password);

            SocketClient client = SocketClient.getClient();
            LoginRequest data = new LoginRequest(username, password);           
            Request request = new Request(RequestType.LOGIN, data);

            try{
                Response response = client.send(request);
                System.out.println(response.getMessage());
                switch (response.getStatus()) {
                    case SUCCESS -> {
                        messageLabel.setStyle("-fx-text-fill: #2fe957;");
                        messageLabel.setText("Logged in");
                    }
                    default -> throw new AuthException(response.getMessage());
                }
            }
            catch (AuthException e){
                messageLabel.setStyle("-fx-text-fill: #ff5656;");
                messageLabel.setText(e.getMessage());
            }
            catch (IOException e){
                messageLabel.setStyle("-fx-text-fill: #ff5656;");
                messageLabel.setText("Log in failed, please try again");
                e.printStackTrace();
            }
        }
        catch(ValidationException e){
            messageLabel.setStyle("-fx-text-fill: #ff5656;");
            messageLabel.setText(e.getMessage());
        }
    }

    // đổi sang scene đăng kí
    @FXML
    private void toRegister(ActionEvent event){ SceneManager.switchScene("register.fxml"); }
}

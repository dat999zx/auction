package com.bidify.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;

import com.bidify.common.model.RegisterRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.utility.SceneManager;
import com.bidify.utility.SocketClient;
import com.bidify.common.util.ValidationUtil;

import java.io.IOException;

public class RegisterController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField nicknameField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField passwordConfirmField;
    @FXML
    private Label messageLabel;
    
    // nút register
    @FXML
    private void handleRegister(){
        try{
            String username = usernameField.getText();
            String nickname = nicknameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String passwordConfirm = passwordConfirmField.getText();
            
            ValidationUtil.validateUsername(username);
            ValidationUtil.validateNickname(nickname);
            ValidationUtil.validateEmail(email);
            ValidationUtil.validatePassword(password);
            if (!passwordConfirm.equals(password)) throw new ValidationException("Password confirmation does not match");

            SocketClient client = SocketClient.getClient();
            RegisterRequest data = new RegisterRequest(username, nickname, email, password);           
            Request request = new Request(RequestType.REGISTER, data);

            try{
                Response response = client.send(request);
                System.out.println(response.getMessage());
                switch (response.getStatus()) {
                    case SUCCESS -> {
                        messageLabel.setStyle("-fx-text-fill: #2fe957;");
                        messageLabel.setText("Register successfully, please login");
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
                messageLabel.setText("Register failed, please try again");
                e.printStackTrace();
            }
        }
        catch(ValidationException e){
            messageLabel.setStyle("-fx-text-fill: #ff5656;");
            messageLabel.setText(e.getMessage());
        }
    }

    // đổi sang scene đăng nhập
    @FXML
    private void toLogin(ActionEvent event){ SceneManager.switchScene("login.fxml"); }
}

package com.bidify.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;

import com.bidify.common.model.RegisterRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.utility.SceneManager;
import com.bidify.common.util.ValidationUtil;
import com.bidify.network.SocketClient;

import java.io.IOException;

public class RegisterController {
    @FXML
    private TextField usernameField;
    @FXML
    private TextField nicknameField;
    @FXML
    private TextField emailField;
    @FXML
    private Label messageLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordFieldVisible;
    @FXML
    private PasswordField passwordConfirmField;
    @FXML
    private ImageView eyeOpen;   // icon for visible mode
    @FXML
    private ImageView eyeClose;  // icon for hidden mode
    // nút register

    private boolean showing = false;


    @FXML
    public void initialize() {
        // sync both pairs
        passwordFieldVisible.textProperty()
            .bindBidirectional(passwordField.textProperty());
    
        // default hidden
        passwordFieldVisible.setVisible(false);
        passwordField.setVisible(true);
    
        eyeOpen.setVisible(true);
        eyeClose.setVisible(false);
    }

    @FXML
    private void handleRegister() {
        try {
            
            String username = usernameField.getText().trim();
            String nickname = nicknameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            
            ValidationUtil.validateUsername(username);
            ValidationUtil.validateNickname(nickname);
            ValidationUtil.validateEmail(email);
            ValidationUtil.validatePassword(password);

            String passwordConfirm = passwordConfirmField.getText();
            if (!password.equals(passwordConfirm)) {
                throw new ValidationException("Confirm password do not match");
            }

            SocketClient client = SocketClient.getClient();
            RegisterRequest data = new RegisterRequest(username, nickname, email, password);
            Request request = new Request(RequestType.REGISTER, data);

            try {
                Response response = client.send(request);
                System.out.println(response.getMessage());

                switch (response.getStatus()) {
                    case SUCCESS -> {
                        messageLabel.setStyle("-fx-text-fill: #2fe957;");
                        messageLabel.setText("Register successfully, please login");
                    }
                    default -> throw new AuthException(response.getMessage());
                }
            } catch (AuthException e) {
                messageLabel.setStyle("-fx-text-fill: #ff5656;");
                messageLabel.setText(e.getMessage());
            } catch (IOException e) {
                messageLabel.setStyle("-fx-text-fill: #ff5656;");
                messageLabel.setText("Cannot connect to server");
                e.printStackTrace();
            }
        } catch (ValidationException e) {
            messageLabel.setStyle("-fx-text-fill: #ff5656;");
            messageLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void toggletext(){
        showing = !showing;

        // toggle main password
        passwordFieldVisible.setVisible(showing);
        passwordField.setVisible(!showing);
    
        // toggle icons
        eyeOpen.setVisible(!showing);
        eyeClose.setVisible(showing);
    
        // focus (just focus main field, enough)
        if (showing) {
            passwordFieldVisible.requestFocus();
            passwordFieldVisible.positionCaret(passwordFieldVisible.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }

    // đổi sang scene đăng nhập
    @FXML
    private void toLogin(ActionEvent event){ SceneManager.switchScene("login.fxml"); }
}
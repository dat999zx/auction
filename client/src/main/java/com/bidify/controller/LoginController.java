package com.bidify.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;

import com.bidify.utility.SceneManager;

import java.io.IOException;

import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.LoginRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.util.ValidationUtil;
import com.bidify.network.SocketClient;
import com.bidify.common.exception.AuthException;

public class LoginController {
    @FXML
    private TextField usernameField;
    @FXML
    private Label messageLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordFieldVisible;
    @FXML
    private ImageView eyeOpen;   // icon for visible mode
    @FXML
    private ImageView eyeClose;  // icon for hidden mode
    // nút login

    private boolean showing = false;

    @FXML
    public void initialize() {
    // sync text
    passwordFieldVisible.textProperty()
    .bindBidirectional(passwordField.textProperty());

    // default state = hidden
        passwordFieldVisible.setVisible(false);
        passwordField.setVisible(true);

        eyeOpen.setVisible(false);
        eyeClose.setVisible(true);
    }
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
                        client.setCurrentUsername(username);
                        messageLabel.setStyle("-fx-text-fill: #2fe957;");
                        messageLabel.setText("Logged in");
                        SceneManager.switchScene("hub.fxml");
                        SceneManager.clearAllCache();
                        
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
                messageLabel.setText("Cannot connect to server");
                e.printStackTrace();
            }
        }
        catch(ValidationException e){
            messageLabel.setStyle("-fx-text-fill: #ff5656;");
            messageLabel.setText(e.getMessage());
        }

    }

    //
    @FXML
    private void toggletext(){

        showing = !showing;

        // toggle fields
        passwordFieldVisible.setVisible(showing);
        passwordField.setVisible(!showing);
    
        // toggle icons
        eyeOpen.setVisible(showing);
        eyeClose.setVisible(!showing);
    
        // fix typing focus (important)
        if (showing) {
            passwordFieldVisible.requestFocus();
            passwordFieldVisible.positionCaret(passwordFieldVisible.getText().length());
        } else {
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getText().length());
        }
    }
    // đổi sang scene đăng kí
    @FXML
    private void toRegister(){ SceneManager.switchScene("register.fxml"); }
}

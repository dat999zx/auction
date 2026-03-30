package com.bidify.controller;

import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.LoginRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.util.ValidationUtil;
import com.bidify.network.SocketClient;
import com.bidify.utility.SceneManager;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

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
    private ImageView passwordEyeOpenIcon;
    @FXML
    private ImageView passwordEyeCloseIcon;

    private boolean showingPassword;

    @FXML
    public void initialize() {
        passwordFieldVisible.textProperty().bindBidirectional(passwordField.textProperty());
        setPasswordVisibility(false);
    }

    @FXML
    private void handleLogin() {
        try {
            showMessage("", false);
            String username = usernameField.getText();
            String password = passwordField.getText();

            ValidationUtil.validateUsername(username);
            ValidationUtil.validatePassword(password);

            SocketClient client = SocketClient.getClient();
            LoginRequest data = new LoginRequest(username, password);
            Request request = new Request(RequestType.LOGIN, data);

            try {
                Response response = client.send(request);
                System.out.println(response.getMessage());
                switch (response.getStatus()) {
                    case SUCCESS -> {
                        client.setCurrentUsername(username);
                        showMessage("Logged in", true);
                        SceneManager.switchScene("hub.fxml");
                        SceneManager.clearAllCache();
                    }
                    default -> throw new AuthException(response.getMessage());
                }
            } catch (AuthException e) {
                showMessage(e.getMessage(), false);
            } catch (IOException e) {
                showMessage("Cannot connect to server", false);
                e.printStackTrace();
            }
        } catch (ValidationException e) {
            showMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void togglePasswordVisibility() {
        setPasswordVisibility(!showingPassword);
    }

    private void setPasswordVisibility(boolean visible) {
        showingPassword = visible;
        passwordFieldVisible.setVisible(visible);
        passwordFieldVisible.setManaged(visible);
        passwordField.setVisible(!visible);
        passwordField.setManaged(!visible);
        passwordEyeOpenIcon.setVisible(visible);
        passwordEyeOpenIcon.setManaged(visible);
        passwordEyeCloseIcon.setVisible(!visible);
        passwordEyeCloseIcon.setManaged(!visible);

        TextField activeField = visible ? passwordFieldVisible : passwordField;
        activeField.requestFocus();
        activeField.positionCaret(activeField.getText().length());
    }

    private void showMessage(String message, boolean success) {
        if (messageLabel == null) return;
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        if (!message.isBlank()) {
            messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
        }
    }

    @FXML
    private void toRegister() {
        SceneManager.switchScene("register.fxml");
    }
}

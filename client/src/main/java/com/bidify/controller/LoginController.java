package com.bidify.controller;

import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.service.AuthClientService;
import com.bidify.utility.SceneManager;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
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
    private final AuthClientService authClientService = new AuthClientService();

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

            Response response = authClientService.login(username, password);
            System.out.println(response.getMessage());
            if (response.getStatus() == com.bidify.common.enums.RequestStatus.SUCCESS) {
                showMessage("Logged in", true);
                SceneManager.clearAllCache();
                SceneManager.switchScene("hub.fxml", false, true);
            }
        } catch (AuthException e) {
            showMessage(e.getMessage(), false);
        } catch (IOException e) {
            showMessage("Cannot connect to server", false);
            e.printStackTrace();
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
        SceneManager.switchScene("register.fxml", true, false);
    }

    @FXML
    private void suprise() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
                showMessage("", true);
                return;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

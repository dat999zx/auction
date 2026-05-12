package com.bidify.controller;

import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.service.AuthClientService;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    @FXML
    private TextField usernameField;
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
        Platform.runLater(() -> {
            passwordFieldVisible.textProperty().bindBidirectional(passwordField.textProperty());
            setPasswordVisibility(false);
        });
    }

    @FXML
    private void handleLogin() {
        try {
            String username = usernameField.getText();
            String password = passwordField.getText();

            ValidationUtil.validateUsername(username);
            ValidationUtil.validatePassword(password);

            Response response = authClientService.login(username, password);
            logger.info(response.getMessage());
            if (response.getStatus() == com.bidify.common.enums.RequestStatus.SUCCESS) {
                NotificationUtil.success("Welcome back, " + username + "!");
                SceneManager.clearAllCache();
                SceneManager.switchScene("hub.fxml", false, true);
            }
        } catch (AuthException | ValidationException e) {
            NotificationUtil.error(e.getMessage());
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server");
            logger.error("Exception occurred", e);
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

    @FXML
    private void toRegister() {
        SceneManager.switchScene("register.fxml", true, false);
    }

    @FXML
    private void suprise() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://www.youtube.com/watch?v=9BalEldzE8o"));
            }
        }
        catch (IOException e) {
            logger.error("Exception occurred", e);
        }
    }
}

package com.bidify.controller;

import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.service.AuthClientService;
import com.bidify.utility.SceneManager;
import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    @FXML
    private TextField usernameField;
    @FXML
    private Label messageLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordFieldVisible;
    @FXML
    private PasswordField passwordConfirmField;
    @FXML
    private TextField passwordConfirmFieldVisible;
    @FXML
    private ImageView passwordEyeOpenIcon;
    @FXML
    private ImageView passwordEyeCloseIcon;

    private boolean showingPassword;
    private final AuthClientService authClientService = new AuthClientService();

    @FXML
    public void initialize() {
        passwordFieldVisible.textProperty().bindBidirectional(passwordField.textProperty());
        passwordConfirmFieldVisible.textProperty().bindBidirectional(passwordConfirmField.textProperty());
        setPasswordVisibility(false);
    }

    @FXML
    private void handleRegister() {
        try {
            showMessage("", false);
            String username = usernameField.getText();
            String password = passwordField.getText();
            String passwordConfirm = passwordConfirmField.getText();

            ValidationUtil.validateUsername(username);
            ValidationUtil.validatePassword(password);
            if (!passwordConfirm.equals(password)) {
                throw new ValidationException("Password confirmation does not match");
            }

            Response response = authClientService.register(username, password);
            logger.info(response.getMessage());
            if (response.getStatus() == com.bidify.common.enums.RequestStatus.SUCCESS) {
                showMessage("Register successfully, please login", true);
            }
        } catch (AuthException | ValidationException e) {
            showMessage(e.getMessage(), false);
        } catch (IOException e) {
            showMessage("Cannot connect to server", false);
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

        passwordConfirmFieldVisible.setVisible(visible);
        passwordConfirmFieldVisible.setManaged(visible);
        passwordConfirmField.setVisible(!visible);
        passwordConfirmField.setManaged(!visible);
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
    private void toLogin(ActionEvent event) {
        SceneManager.switchScene("login.fxml", true, false);
    }
}

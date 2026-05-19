package com.bidify.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.service.AuthClientService;
import com.bidify.utility.NotificationUtil;
import com.bidify.utility.SceneManager;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;

public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    @FXML
    private TextField usernameField;
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

    // dùng để khởi tạo
    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            passwordFieldVisible.textProperty().bindBidirectional(passwordField.textProperty());
            passwordConfirmFieldVisible.textProperty().bindBidirectional(passwordConfirmField.textProperty());
            // dùng để thiết lập mật khẩu visibility
            setPasswordVisibility(false);
        });
        usernameField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
                event.consume(); // Prevents the event from traveling further
            }
        });
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                passwordConfirmField.requestFocus();
                event.consume(); // Prevents the event from traveling further
            }
        });
        passwordConfirmField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                // dùng để xử lý đăng ký
                handleRegister();
                event.consume(); // Prevents the event from traveling further
            }
        });
    }

    // dùng để xử lý đăng ký
    @FXML
    private void handleRegister() {
        try {
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
                NotificationUtil.success("Register successfully, please login");
            }
        } catch (AuthException | ValidationException e) {
            NotificationUtil.error(e.getMessage());
        } catch (IOException e) {
            NotificationUtil.error("Cannot connect to server");
            logger.error("Exception occurred", e);
        }
    }

    // dùng để bật tắt mật khẩu visibility
    @FXML
    private void togglePasswordVisibility() {
        // dùng để thiết lập mật khẩu visibility
        setPasswordVisibility(!showingPassword);
    }

    // dùng để thiết lập mật khẩu visibility
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

    // dùng để chuyển thành đăng nhập
    @FXML
    private void toLogin(ActionEvent event) {
        SceneManager.switchScene("login.fxml", true, false);
    }
}

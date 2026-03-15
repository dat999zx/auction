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
import com.bidify.common.exception.ValidationException;
import com.bidify.utility.SceneManager;
import com.bidify.utility.SocketClient;

import java.io.IOException;

public class RegisterController {
    @FXML
    private TextField nameField;
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
            String name = nameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            String passwordConfirm = passwordConfirmField.getText();

            if (name.length() < 3) throw new ValidationException("Username is too short");
            if (email.isEmpty()) throw new ValidationException("Email must not be empty");
            if (!email.contains("@") || email.contains(" ")) throw new ValidationException("Invalid email");
            if (password.contains(" ")) throw new ValidationException("Invalid password");
            if (password.length() < 5) throw new ValidationException("Password is too short");
            if (!passwordConfirm.equals(password)) throw new ValidationException("Password confirmation did not match");

            SocketClient client = SocketClient.getClient();
            RegisterRequest data = new RegisterRequest(name, email, password);           
            Request request = new Request(RequestType.REGISTER, data);

            try{
                Response response = client.send(request);
                System.out.println(response.getMessage());
            }
            catch (IOException e){
                messageLabel.setStyle("-fx-text-fill: red;");
                messageLabel.setText("Register failed, please try again");
                e.printStackTrace();
            }
        }
        catch(ValidationException e){
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(e.getMessage());
            System.out.println("Validation error: " + e.getMessage());
        }
    }

    // đổi sang scene đăng nhập
    @FXML
    private void toLogin(ActionEvent event){ SceneManager.switchScene("login.fxml"); }
}

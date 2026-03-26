package com.bidify.controller;

import java.io.IOException;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.LogoutRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.network.SocketClient;
import com.bidify.utility.SceneManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class HubController {
    @FXML
    private Button auctionsButton;

    @FXML
    private Button galleriesButton;

    @FXML
    private Button privateSalesButton;

    @FXML
    private void initialize() {
        setActiveTopNav(auctionsButton);
    }

    @FXML
    private void handleSelection(ActionEvent event) {
        if (event.getSource() instanceof Button selectedButton) {
            setActiveTopNav(selectedButton);
        }
    }

    @FXML
    private void handleLogout() {
        SocketClient client = SocketClient.getClient();
        String currentUsername = client.getCurrentUsername();

        if (currentUsername == null || currentUsername.isBlank()) {
            SceneManager.clearAllCache();
            SceneManager.switchScene("login.fxml", false);
            return;
        }

        Request request = new Request(RequestType.LOGOUT, new LogoutRequest(currentUsername));
        try {
            Response response = client.send(request);
            if (response.getStatus() == RequestStatus.SUCCESS) {
                client.setCurrentUsername(null);
                SceneManager.clearAllCache();
                SceneManager.switchScene("login.fxml", false);
                return;
            }
            System.err.println("Logout failed: " + response.getMessage());
        } catch (IOException e) {
            System.err.println("Cannot connect to server while logging out");
            e.printStackTrace();
        }
    }

    private void setActiveTopNav(Button activeButton) {
        Button[] topNavButtons = { auctionsButton, galleriesButton, privateSalesButton };

        for (Button button : topNavButtons) {
            if (button == null) {
                continue;
            }
            button.getStyleClass().removeAll("top-link", "top-link-active");
            button.getStyleClass().add(button == activeButton ? "top-link-active" : "top-link");
        }
    }
}

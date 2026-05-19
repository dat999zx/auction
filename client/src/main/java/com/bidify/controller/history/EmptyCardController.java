package com.bidify.controller.history;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class EmptyCardController {
    @FXML
    private HBox card;

    @FXML
    private Label messageLabel;

    // dùng để thiết lập tin nhắn
    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}

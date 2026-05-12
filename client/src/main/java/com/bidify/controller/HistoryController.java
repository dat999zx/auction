package com.bidify.controller;

import com.bidify.utility.MissionBarUtil;
import com.bidify.utility.NavPage;
import javafx.application.Platform;
import javafx.fxml.FXML;

public class HistoryController {

    @FXML
    private void initialize() {
        Platform.runLater(this::bindTopBar);
    }

    private void bindTopBar() {
        MissionBarUtil.setup(NavPage.HISTORY, false, null);
    }
}

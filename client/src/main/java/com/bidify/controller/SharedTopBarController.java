package com.bidify.controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class SharedTopBarController {
    @FXML
    private HBox searchContainer;

    @FXML
    private Button exploreButton;

    @FXML
    private TextField searchBar;

    @FXML
    private Button auctionsButton;

    @FXML
    private Button createAuctionButton;

    @FXML
    private Button logoutButton;

    @FXML
    private Button logoutLinkButton;

    @FXML
    private Label avatarText;

    public Button getExploreButton() {
        return exploreButton;
    }

    public TextField getSearchBar() {
        return searchBar;
    }

    public Button getAuctionsButton() {
        return auctionsButton;
    }

    public Button getCreateAuctionButton() {
        return createAuctionButton;
    }

    public Button getLogoutButton() {
        return logoutButton;
    }

    public Button getLogoutLinkButton() {
        return logoutLinkButton;
    }

    public void setShowExplore(boolean visible) {
        exploreButton.setManaged(visible);
        exploreButton.setVisible(visible);
    }

    public void setShowSearch(boolean visible) {
        searchContainer.setManaged(visible);
        searchContainer.setVisible(visible);
    }

    public void setUseInlineLogout(boolean useInlineLogout) {
        logoutButton.setManaged(useInlineLogout);
        logoutButton.setVisible(useInlineLogout);
        logoutLinkButton.setManaged(!useInlineLogout);
        logoutLinkButton.setVisible(!useInlineLogout);
    }

    public void setSelectionHandler(EventHandler<ActionEvent> handler) {
        auctionsButton.setOnAction(handler);
        createAuctionButton.setOnAction(handler);
        logoutLinkButton.setOnAction(handler);
    }

    public void setExploreHandler(EventHandler<ActionEvent> handler) {
        exploreButton.setOnAction(handler);
    }

    public void setLogoutHandler(EventHandler<ActionEvent> handler) {
        logoutButton.setOnAction(handler);
    }

    public void setAvatarText(String value) {
        avatarText.setText(value == null || value.isBlank() ? "U" : value);
    }
}

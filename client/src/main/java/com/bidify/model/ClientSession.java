package com.bidify.model;

import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.dto.UserDto;

public final class ClientSession {
    private static final ClientSession instance = new ClientSession();

    private String currentUsername;
    private UserDto currentUser;
    private List<AuctionDto> auctions = new ArrayList<>();

    private ClientSession() {}

    public static ClientSession getInstance() {
        return instance;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public UserDto getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserDto currentUser) {
        this.currentUser = currentUser;
        this.currentUsername = currentUser == null ? null : currentUser.getUsername();
    }

    public void clear() {
        currentUsername = null;
        currentUser = null;
    }
}

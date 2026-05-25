package com.bidify.model;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.UserRole;

public final class ClientSession {
    private static final ClientSession instance = new ClientSession();

    private String currentUsername;
    private UserDto currentUser;

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

    public boolean isLoggedIn() {
        return currentUsername != null && !currentUsername.isBlank();
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    public void clear() {
        currentUsername = null;
        currentUser = null;
    }
}

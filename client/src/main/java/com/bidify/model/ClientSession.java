package com.bidify.model;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.UserRole;

public final class ClientSession {
    private static final ClientSession instance = new ClientSession();

    private String currentUsername;
    private UserDto currentUser;

    // dùng để tạo một đối tượng ClientSession
    private ClientSession() {}

    // dùng để lấy đối tượng Singleton
    public static ClientSession getInstance() {
        return instance;
    }

    // dùng để lấy current username
    public String getCurrentUsername() {
        return currentUsername;
    }

    // dùng để thiết lập current username
    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    // dùng để lấy current người dùng
    public UserDto getCurrentUser() {
        return currentUser;
    }

    // dùng để thiết lập current người dùng
    public void setCurrentUser(UserDto currentUser) {
        this.currentUser = currentUser;
        this.currentUsername = currentUser == null ? null : currentUser.getUsername();
    }

    // dùng để kiểm tra xem logged trong
    public boolean isLoggedIn() {
        return currentUsername != null && !currentUsername.isBlank();
    }

    // dùng để kiểm tra xem quản trị viên (admin)
    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }

    // dùng để xóa sạch
    public void clear() {
        currentUsername = null;
        currentUser = null;
    }
}

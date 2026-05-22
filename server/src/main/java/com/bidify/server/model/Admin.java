package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;

public class Admin extends User {
    // dùng để tạo một đối tượng Admin
    public Admin(String username, String nickname, String password) {
        // dùng để super
        super(username, nickname, password);
        // dùng để thiết lập vai trò
        setRole(UserRole.ADMIN);
    }

    @Override
    public boolean isAdmin() {
        return true;
    }

    // dùng để tạo một đối tượng Admin
    public Admin(String username, String nickname, String password, String email, String phone, UserStatus status, LocalDateTime createdAt, LocalDateTime lastLogin, double wallet) {
        super(username, nickname, password, email, phone, status, UserRole.ADMIN, createdAt, lastLogin, wallet);
    }

    public Admin(
        String username,
        String nickname,
        String password,
        String email,
        String phone,
        String profileImageId,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastLogin,
        double wallet
    ) {
        super(username, nickname, password, email, phone, profileImageId, status, UserRole.ADMIN, createdAt, lastLogin, wallet);
    }
}

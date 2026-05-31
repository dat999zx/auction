package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;

// Tài khoản admin — kế thừa User nhưng có toàn quyền quản trị hệ thống
public class Admin extends User {
    public Admin(String username, String nickname, String password) {
        super(username, nickname, password);
        setRole(UserRole.ADMIN);
    }

    @Override
    public boolean isAdmin() {
        return true;
    }

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

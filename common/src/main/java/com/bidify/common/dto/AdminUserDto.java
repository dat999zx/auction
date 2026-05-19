package com.bidify.common.dto;

import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;

public class AdminUserDto {
    private String username;
    private String nickname;
    private UserStatus status;
    private UserRole role;

    // dùng để tạo một đối tượng AdminUserDto
    public AdminUserDto(String username, String nickname, UserStatus status, UserRole role) {
        this.username = username;
        this.nickname = nickname;
        this.status = status;
        this.role = role;
    }

    // dùng để lấy username
    public String getUsername() { return username; }
    // dùng để lấy biệt danh
    public String getNickname() { return nickname; }
    // dùng để lấy trạng thái
    public UserStatus getStatus() { return status; }
    // dùng để lấy vai trò
    public UserRole getRole() { return role; }
}

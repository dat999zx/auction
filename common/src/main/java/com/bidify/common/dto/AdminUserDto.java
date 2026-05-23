package com.bidify.common.dto;

import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;

public class AdminUserDto {
    private String username;
    private String nickname;
    private UserStatus status;
    private UserRole role;

    public AdminUserDto(String username, String nickname, UserStatus status, UserRole role) {
        this.username = username;
        this.nickname = nickname;
        this.status = status;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public UserStatus getStatus() { return status; }
    public UserRole getRole() { return role; }
}

package com.bidify.common.model;

// Yêu cầu xem trang cá nhân công khai của 1 người dùng
public class PublicProfileRequest {
    // Username của người dùng muốn xem trang cá nhân
    private String username;

    public PublicProfileRequest(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
}

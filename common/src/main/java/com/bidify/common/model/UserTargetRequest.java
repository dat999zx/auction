package com.bidify.common.model;

// Yêu cầu thao tác trên 1 user cụ thể — dùng cho các hành động admin (ban/unban/xem thông tin)
public class UserTargetRequest {
    // Username của user mục tiêu (để ban, unban...)
    private String username;

    public UserTargetRequest(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
}

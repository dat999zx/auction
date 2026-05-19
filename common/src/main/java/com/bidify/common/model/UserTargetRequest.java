package com.bidify.common.model;

public class UserTargetRequest {
    private String username;

    // dùng để tạo một đối tượng UserTargetRequest
    public UserTargetRequest(String username) {
        this.username = username;
    }

    // dùng để lấy username
    public String getUsername() { return username; }
}

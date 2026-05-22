package com.bidify.common.model;

public class PublicProfileRequest {
    private String username;

    // dùng để tạo một đối tượng PublicProfileRequest
    public PublicProfileRequest() {}

    // dùng để tạo một đối tượng PublicProfileRequest
    public PublicProfileRequest(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
}

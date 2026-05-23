package com.bidify.common.model;

public class PublicProfileRequest {
    private String username;

    public PublicProfileRequest(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
}

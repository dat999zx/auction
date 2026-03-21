package com.bidify.common.model;

public class LogoutRequest {
    private String username;

    public LogoutRequest(String name){
        this.username = name;
    }
    public String getUsername() { return username; }
}

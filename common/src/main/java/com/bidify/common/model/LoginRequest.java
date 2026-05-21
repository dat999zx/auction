package com.bidify.common.model;

public class LoginRequest {
    private String username, password;

    // dùng để tạo một đối tượng LoginRequest
    public LoginRequest(String username, String password){
        this.username = username;
        this.password = password;
    }

    public String getUsername(){ return username; }
    public String getPassword(){ return password; }
}

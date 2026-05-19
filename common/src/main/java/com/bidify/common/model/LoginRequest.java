package com.bidify.common.model;

public class LoginRequest {
    private String username, password;

    // dùng để tạo một đối tượng LoginRequest
    public LoginRequest(String username, String password){
        this.username = username;
        this.password = password;
    }

    // dùng để lấy username
    public String getUsername(){ return username; }
    // dùng để lấy mật khẩu
    public String getPassword(){ return password; }
}

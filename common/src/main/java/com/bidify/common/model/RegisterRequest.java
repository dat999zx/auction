package com.bidify.common.model;

public class RegisterRequest {
    private String username, password, nickname;

    // dùng để tạo một đối tượng RegisterRequest
    public RegisterRequest(String username, String password){
        this.username = username;
        this.nickname = username;
        this.password = password;
    }

    // dùng để lấy username
    public String getUsername(){ return username; }
    // dùng để lấy biệt danh
    public String getNickname(){ return nickname; }
    // dùng để lấy mật khẩu
    public String getPassword(){ return password; }
}

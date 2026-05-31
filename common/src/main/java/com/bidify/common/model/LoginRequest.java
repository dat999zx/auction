package com.bidify.common.model;

// Dữ liệu gửi lên khi đăng nhập
public class LoginRequest {
    // Tên tài khoản
    private String username;
    // Mật khẩu (gửi dạng thô, server sẽ hash và so sánh)
    private String password;

    public LoginRequest(String username, String password){
        this.username = username;
        this.password = password;
    }

    public String getUsername(){ return username; }
    public String getPassword(){ return password; }
}

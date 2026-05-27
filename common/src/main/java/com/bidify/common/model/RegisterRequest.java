package com.bidify.common.model;

// Dữ liệu gửi lên khi đăng ký tài khoản mới
public class RegisterRequest {
    // Tên tài khoản (dùng để đăng nhập)
    private String username;
    // Mật khẩu
    private String password;
    // Tên hiển thị (mặc định giống username)
    private String nickname;

    public RegisterRequest(String username, String password){
        this.username = username;
        this.nickname = username;
        this.password = password;
    }

    public String getUsername(){ return username; }
    public String getNickname(){ return nickname; }
    public String getPassword(){ return password; }
}

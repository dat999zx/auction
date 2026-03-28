package com.bidify.server.model;

import java.time.LocalDateTime; // dùng để theo dõi thời điểm mà account được khởi tạo, đăng nhập -> quản lý account

import com.bidify.common.enums.UserStatus;

public class User {
    private final String createdAt;
    private String nickname, username, password, email, phoneNumber, lastLogin;
    private UserStatus status;
    private double wallet;

    // Đăng kí tài khoản
    public User(String username, String nickname, String password){
        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now().toString();
        this.lastLogin = null;
        this.wallet = 0;
    }

    // load lại dữ liệu người dùng
     public User(String username, String nickname, String password, String email, String phone, UserStatus status, String createdAt, String lastLogin, double wallet){
        this.nickname = nickname;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phone;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.wallet = wallet;
    }

    public String getCreatedAt() { return createdAt; }
    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public double getWallet() { return wallet; }
    public void setWallet(double wallet) { this.wallet = wallet; }
}

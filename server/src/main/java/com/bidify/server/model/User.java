package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.enums.UserStatus;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.exception.InsufficientBalanceException;

public class User extends Entity {
    private String nickname;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;

    private LocalDateTime lastLogin;
    private UserStatus status;

    // số tiền hiện có trong tài khoản
    private double wallet;

    // số tiền đang bị khóa do đặt bid (CHỈ CHO MỤC ĐÍCH HIỂN THỊ THÔNG TIN, KHÔNG LIÊN QUAN ĐẾN LOGIC)
    private double lockedWallet;

    // Đăng kí tài khoản
    public User(String username, String nickname, String password) {
        super(username, LocalDateTime.now());

        this.username = username;
        this.nickname = nickname;
        this.password = password;

        this.status = UserStatus.ACTIVE;
        this.lastLogin = null;
        this.wallet = 0;
    }

    // Load dữ liệu người dùng từ database
    public User(
        String username,
        String nickname,
        String password,
        String email,
        String phone,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime lastLogin,
        double wallet
    ) {
        super(username, createdAt);

        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.email = email;
        this.phoneNumber = phone;

        this.status = status;
        this.lastLogin = lastLogin;
        this.wallet = wallet;
    }

    public synchronized void addWallet(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Wallet amount");
        wallet += amount;
    }

    public synchronized void subtractWallet(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Wallet amount");

        if (amount > wallet)
            throw new InsufficientBalanceException("Insufficient balance");
        wallet -= amount;
    }

    public synchronized void modifyLockedWallet(double lockedWallet) {
        this.lockedWallet += lockedWallet;
    }

    public synchronized void deposit(double amount) {
        addWallet(amount);
    }

    public synchronized void withdraw(double amount) {
        subtractWallet(amount);
    }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    public double getWallet() { return wallet; }
    public double getLockedWallet() { return lockedWallet; }
    public void setLockedWallet(double lockedWallet) { this.lockedWallet = lockedWallet; }
}
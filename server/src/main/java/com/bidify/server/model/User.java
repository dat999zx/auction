package com.bidify.server.model;

import java.time.LocalDateTime; // dùng để theo dõi thời điểm mà account được khởi tạo, đăng nhập -> quản lý account

import com.bidify.common.enums.UserStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.exception.InsufficientBalanceException;

public class User extends Entity {
    private String nickname, username, password, email, phoneNumber;
    private LocalDateTime lastLogin;
    private UserStatus status;
    private double wallet; // tổng tiền đang có
    private double lockedWallet; // số tiền bị đóng băng khi đã dùng để bid

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

    // load lại dữ liệu người dùng
     public User(String username, String nickname, String password, String email, String phone, UserStatus status, LocalDateTime createdAt, LocalDateTime lastLogin, double wallet) {
        super(username, createdAt);
        this.nickname = nickname;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phone;
        this.status = status;
        this.lastLogin = lastLogin;
        this.wallet = wallet;
    }

    public synchronized double getAvailableBalance() { return wallet - lockedWallet; }
    public synchronized void lockBalance(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Lock balance amount");
        if (amount > getAvailableBalance())
            throw new InsufficientBalanceException("Insufficient available balance");
        lockedWallet += amount;
    }
    public synchronized void unlockBalance(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Unlock balance amount");
        if (amount > lockedWallet)
            throw new InsufficientBalanceException("Insufficient locked balance");
        lockedWallet -= amount;
    }
    public synchronized void payWinAuction(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Pay amount");
        if (lockedWallet < amount)
            throw new InsufficientBalanceException("Insufficient locked balance");
        wallet -= amount;
        lockedWallet -= amount;
    }
    public synchronized void deposit(double amount) {
        if (amount <= 0)
            throw new ValidationException("Deposit amount must be positive");
        wallet += amount;
    }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
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
    public void setLockedWallet(double lockedWallet) { this.lockedWallet = lockedWallet; }
    public double getLockedWallet() { return lockedWallet; }
}

package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.TimeUtil;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;
import com.bidify.server.exception.ServerTimeOutException;

public class User extends Entity {
    // khóa Object User để tránh race condition
    private final ReentrantLock lock = new ReentrantLock();

    private String nickname;
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private String profileImageId;

    private LocalDateTime lastLogin;
    private UserStatus status;
    private UserRole role;
    private Wallet wallet;

    // dùng để tạo một đối tượng User
    public User(String username, String nickname, String password) {
        super(username, TimeUtil.nowInVietnam());

        this.username = username;
        this.nickname = nickname;
        this.password = password;

        this.status = UserStatus.ACTIVE;
        this.role = UserRole.USER;
        this.lastLogin = null;
        this.wallet = new Wallet(0);
        this.profileImageId = null;
    }

     // dùng để tạo một đối tượng User
     public User(String username, String nickname, String password, String email, String phone, UserStatus status, UserRole role, LocalDateTime createdAt, LocalDateTime lastLogin, double balance) {
        // dùng để super
        super(username, createdAt);

        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.email = email;
        this.phoneNumber = phone;

        this.status = status;
        this.role = role == null ? UserRole.USER : role;
        this.lastLogin = lastLogin;
        this.wallet = new Wallet(balance);
        this.profileImageId = null;
    }

    // dùng để tạo một đối tượng User
    public User(String username, String nickname, String password, String email, String phone, String profileImageId, UserStatus status, UserRole role, LocalDateTime createdAt, LocalDateTime lastLogin, double balance) {
        // dùng để super
        super(username, createdAt);

        this.username = username;
        this.nickname = nickname;
        this.password = password;
        this.email = email;
        this.phoneNumber = phone;
        this.profileImageId = profileImageId;

        this.status = status;
        this.role = role == null ? UserRole.USER : role;
        this.lastLogin = lastLogin;
        this.wallet = new Wallet(balance);
    }

    public String getProfileImageId() { return profileImageId; }
    public void setProfileImageId(String profileImageId) { this.profileImageId = profileImageId; }

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
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role == null ? UserRole.USER : role; }
    public boolean isAdmin() { return false; }
    public Wallet getWallet() { return wallet; }

    // dùng để khóa
    public void lock() { lock.lock(); }
    // dùng để mở khóa
    public void unlock() { lock.unlock(); }
    // dùng để try khóa đồng bộ
    public void tryLock(int timeout) throws ServerTimeOutException {
        try {
            lock.tryLock(timeout, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerTimeOutException();
        }
    }
}

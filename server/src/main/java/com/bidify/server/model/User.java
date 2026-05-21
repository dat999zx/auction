package com.bidify.server.model;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;
import com.bidify.server.exception.ServerTimeOutException;

public class User extends Entity {
    // khÃ³a Object User Ä‘á»ƒ trÃ¡nh race condition
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
        super(username, LocalDateTime.now());

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

    // dùng để lấy ảnh đại diện ID
    public String getProfileImageId() { return profileImageId; }
    // dùng để thiết lập ảnh đại diện ID
    public void setProfileImageId(String profileImageId) { this.profileImageId = profileImageId; }

    // dùng để lấy last đăng nhập
    public LocalDateTime getLastLogin() { return lastLogin; }
    // dùng để thiết lập last đăng nhập
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    // dùng để lấy biệt danh
    public String getNickname() { return nickname; }
    // dùng để thiết lập biệt danh
    public void setNickname(String nickname) { this.nickname = nickname; }
    // dùng để lấy username
    public String getUsername() { return username; }
    // dùng để lấy mật khẩu
    public String getPassword() { return password; }
    // dùng để thiết lập mật khẩu
    public void setPassword(String password) { this.password = password; }
    // dùng để lấy email
    public String getEmail() { return email; }
    // dùng để thiết lập email
    public void setEmail(String email) { this.email = email; }
    // dùng để lấy phone number
    public String getPhoneNumber() { return phoneNumber; }
    // dùng để thiết lập phone number
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    // dùng để lấy trạng thái
    public UserStatus getStatus() { return status; }
    // dùng để thiết lập trạng thái
    public void setStatus(UserStatus status) { this.status = status; }
    // dùng để lấy vai trò
    public UserRole getRole() { return role; }
    // dùng để thiết lập vai trò
    public void setRole(UserRole role) { this.role = role == null ? UserRole.USER : role; }
    // dùng để lấy ví
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

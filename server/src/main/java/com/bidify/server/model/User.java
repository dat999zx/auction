package com.bidify.server.model;

import java.time.LocalDateTime; // dùng để theo dõi thời điểm mà account được khởi tạo, đăng nhập -> quản lý account

import com.bidify.common.enums.Role;
import com.bidify.common.enums.UserStatus;

public class User {
    private final String createdAt;
    private String lastLogin;
    private String nickname, username, password, email, phoneNumber;
    private Role role;
    private UserStatus status;
    private boolean inSession;

    // Đăng kí tài khoản
    public User(String nickname, String username, String password, String email){
        this.nickname = nickname;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = null;
        
        this.role = Role.USER;
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now().toString();
        this.lastLogin = null;
        this.inSession = false;
    }

    // load lại dữ liệu người dùng
    public User(String nickname, String username, String password, String email, String phone, Role role, UserStatus status, String createdAt, String lastLogin){
        this.nickname = nickname;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phone;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.inSession = false;
    }

    public String getNickname(){ return nickname; }
    public String getUsername(){ return username; }
    public String getPassword(){ return password; }
    public String getEmail(){
        if (email != null) return email;
        else return "Email is not verified";
    }
    public String getPhone(){ return phoneNumber; }
    public Role getRole(){ return role; }
    public UserStatus getStatus(){ return status; }
    public String getLastLogin(){ return lastLogin; }
    public String getCreatedAt(){ return createdAt; }

    public void setUsername(String name){ this.username = name; }
    public void setPassword(String pass){ this.password = pass; }
    public void setEmail(String email){ this.email = email; }
    public void setPhone(String phone){ this.phoneNumber = phone; }
    public void setNickname(String name){ this.nickname = name; }
    public void setRole(Role role){ this.role = role; }
    public void setStatus(UserStatus status){ this.status = status; }
    public void setLastLogin(String lastlogin){ this.lastLogin = lastlogin; }
    public void setInSession(boolean state){ this.inSession = state; }
    
    public boolean isInSession(){ return inSession; }
    public boolean isAdmin(){ return role == Role.ADMIN; }
    public boolean isActive(){ return status == UserStatus.ACTIVE; }

}

package com.bidify.common.model;

import com.bidify.common.enums.UserStatus;

/**
 * Abstract User class - represents a user in the system
 */
public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;
    
    protected String username;
    protected String email;
    protected String password;
    protected String fullName;
    protected String phoneNumber;
    protected String address;
    protected UserStatus status; // Trạng thái hiện tại người dùng
    protected double reputation; // Điểm uy tín của người dùng
    
    public User() {
        super();
        this.reputation = 0.0;
        this.status = UserStatus.ACTIVE;
    }
    
    public User(String username, String email, String password, String fullName) {
        super();
        this.username = username;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.reputation = 0.0;
        this.status = UserStatus.ACTIVE;
    }
    
    // Abstract methods
    public abstract String getUserType();
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
    }
    
    public double getReputation() {
        return reputation;
    }
    
    public void setReputation(double reputation) {
        this.reputation = reputation;
    }
}

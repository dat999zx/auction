package com.bidify.common.dto;

import com.bidify.common.enums.UserRole;

public class UserDto {
    private String username;
    private String nickname;
    private WalletDto wallet;
    private UserRole role;
    private String profileImageBase64;

    // dùng để tạo một đối tượng UserDto
    public UserDto(String username, String nickname, WalletDto wallet, UserRole role){
        this.username = username;
        this.nickname = nickname;
        this.wallet = wallet;
        this.role = role;
        this.profileImageBase64 = null;
    }

    // dùng để tạo một đối tượng UserDto
    public UserDto(String username, String nickname, WalletDto wallet, UserRole role, String profileImageBase64){
        this.username = username;
        this.nickname = nickname;
        this.wallet = wallet;
        this.role = role;
        this.profileImageBase64 = profileImageBase64;
    }
    
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public WalletDto getWallet() { return wallet; }
    public UserRole getRole() { return role; }
    public String getProfileImageBase64() { return profileImageBase64; }
}


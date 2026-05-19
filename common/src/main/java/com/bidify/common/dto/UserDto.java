package com.bidify.common.dto;

import com.bidify.common.enums.UserRole;

public class UserDto {
    private String username;
    private String nickname;
    private WalletDto wallet;
    private UserRole role;

    // dùng để tạo một đối tượng UserDto
    public UserDto(String username, String nickname, WalletDto wallet, UserRole role){
        this.username = username;
        this.nickname = nickname;
        this.wallet = wallet;
        this.role = role;
    }
    
    // dùng để lấy username
    public String getUsername() { return username; }
    // dùng để lấy biệt danh
    public String getNickname() { return nickname; }
    // dùng để lấy ví
    public WalletDto getWallet() { return wallet; }
    // dùng để lấy vai trò
    public UserRole getRole() { return role; }
}

package com.bidify.common.dto;

import com.bidify.common.enums.UserRole;

public class UserDto {
    private String username;
    private String nickname;
    private WalletDto wallet;
    private UserRole role;

    public UserDto(String username, String nickname, WalletDto wallet, UserRole role){
        this.username = username;
        this.nickname = nickname;
        this.wallet = wallet;
        this.role = role;
    }
    
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public WalletDto getWallet() { return wallet; }
    public UserRole getRole() { return role; }
}

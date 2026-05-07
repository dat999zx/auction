package com.bidify.common.dto;

public class UserDto {
    private String username, nickname;
    private double wallet, lockedWallet;

    public UserDto(String username, String nickname, double wallet, double lockedWallet) {
        this.username = username;
        this.nickname = nickname;
        this.wallet = wallet;
        this.lockedWallet = lockedWallet;
    }
    
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public double getWallet() { return wallet; }
    public double getLockedWallet() { return lockedWallet; }
}

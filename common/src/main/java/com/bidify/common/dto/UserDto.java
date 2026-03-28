package com.bidify.common.dto;

public class UserDto {
    private String username, nickname;
    private double wallet;

    public UserDto(String username, String nickname, double wallet){
        this.username = username;
        this.nickname = nickname;
        this.wallet = wallet;
    }
    
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public double getWallet() { return wallet; }
}

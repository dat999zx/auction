package com.bidify.common.dto;

public class UserDto {
    private String username;
    private String nickname;
    private WalletDto wallet;

    public UserDto(String username, String nickname, WalletDto wallet){
        this.username = username;
        this.nickname = nickname;
        this.wallet = wallet;
    }
    
    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public WalletDto getWallet() { return wallet; }
}

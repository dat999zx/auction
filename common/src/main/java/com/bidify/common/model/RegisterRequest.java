package com.bidify.common.model;

public class RegisterRequest {
    private String username, password, nickname;

    public RegisterRequest(String username, String nickname, String password){
        this.username = username;
        this.nickname = nickname;
        this.password = password;
    }

    public String getUsername(){ return username; }
    public String getNickname(){ return nickname; }
    public String getPassword(){ return password; }
}

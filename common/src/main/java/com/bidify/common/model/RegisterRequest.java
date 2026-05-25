package com.bidify.common.model;

public class RegisterRequest {
    private String username;
    private String password;
    private String nickname;

    public RegisterRequest(String username, String password){
        this.username = username;
        this.nickname = username;
        this.password = password;
    }

    public String getUsername(){ return username; }
    public String getNickname(){ return nickname; }
    public String getPassword(){ return password; }
}

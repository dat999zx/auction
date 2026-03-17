package com.bidify.common.model;

public class RegisterRequest {
    private String username, email, password, nickname;

    public RegisterRequest(String username, String nickname, String email, String password){
        this.username = username;
        this.nickname = nickname;
        this.email = email;
        this.password = password;
    }

    public String getUsername(){ return username; }
    public String getNickname(){ return nickname; }
    public String getEmail(){ return email; }
    public String getPassword(){ return password; }
}

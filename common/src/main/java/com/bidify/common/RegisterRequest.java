package com.bidify.common;

public class RegisterRequest {
    private String username, email, password;

    public RegisterRequest(){}
    public RegisterRequest(String username, String email, String password){
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername(){ return username; }
    public String getEmail(){ return email; }
    public String password(){ return password; }
}

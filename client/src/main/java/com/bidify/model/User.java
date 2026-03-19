package com.bidify.model;

public class User {
    private String username, password; 
    private Role userRole;
    private String nickname = "";

    public User(String username, String password, Role role){
        this.username = username;
        this.password = password;
        this.userRole = role; 
    }
    public void setNickname(String name) { this.nickname = name; }

    public String getNickname() { return nickname; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return userRole; }
}

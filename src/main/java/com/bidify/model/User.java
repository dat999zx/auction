package com.bidify.model;

public class User {
    private String userId;
    private String username, password; 
    private Role userRole;
    private String nickname = "";

    public User(String id, String username, String password, Role role){
        this.userId = id;
        this.username = username;
        this.password = password;
        this.userRole = role; 
    }
    public void setNickname(String name) { this.nickname = name; }

    public String getNickname() { return nickname; }
    public String getId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Role getRole() { return userRole; }

    public static void main(String[] args) {
        User minh = new User("0001", "minhpham", "12345", Role.ADMIN);
        minh.setNickname("blablabla");
        System.out.println(minh.getUsername() + "'s role: " + minh.getRole());
    }
}

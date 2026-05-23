package com.bidify.common.model;

public class UpdateProfileRequest {
    private String nickname;
    private Double wallet;
    private String profileImageBase64;

    private String email;
    private String phoneNumber;

    public UpdateProfileRequest(String nickname, Double wallet) {
        this(nickname, wallet, null, null, null);
    }

    public UpdateProfileRequest(String nickname, Double wallet, String profileImageBase64) {
        this(nickname, wallet, profileImageBase64, null, null);
    }

    public UpdateProfileRequest(String nickname, Double wallet, String profileImageBase64, String email, String phoneNumber) {
        this.nickname = nickname;
        this.wallet = wallet;
        this.profileImageBase64 = profileImageBase64;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getNickname() { return nickname; }
    public Double getWallet() { return wallet; }
    public String getProfileImageBase64() { return profileImageBase64; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
}


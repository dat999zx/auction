package com.bidify.common.model;

public class UpdateProfileRequest {
    private String nickname;
    private Double wallet;
    private String profileImageBase64;

    // dùng để tạo một đối tượng UpdateProfileRequest
    public UpdateProfileRequest() {}

    // dùng để tạo một đối tượng UpdateProfileRequest
    public UpdateProfileRequest(String nickname, Double wallet) {
        this.nickname = nickname;
        this.wallet = wallet;
        this.profileImageBase64 = null;
    }

    // dùng để tạo một đối tượng UpdateProfileRequest
    public UpdateProfileRequest(String nickname, Double wallet, String profileImageBase64) {
        this.nickname = nickname;
        this.wallet = wallet;
        this.profileImageBase64 = profileImageBase64;
    }

    // dùng để lấy biệt danh
    public String getNickname() { return nickname; }
    // dùng để lấy ví
    public Double getWallet() { return wallet; }
    // dùng để lấy ảnh đại diện
    public String getProfileImageBase64() { return profileImageBase64; }
}


package com.bidify.common.model;

public class UpdateProfileRequest {
    private String nickname;
    private Double wallet;

    // dùng để tạo một đối tượng UpdateProfileRequest
    public UpdateProfileRequest() {}

    // dùng để tạo một đối tượng UpdateProfileRequest
    public UpdateProfileRequest(String nickname, Double wallet) {
        this.nickname = nickname;
        this.wallet = wallet;
    }

    // dùng để lấy biệt danh
    public String getNickname() { return nickname; }
    // dùng để lấy ví
    public Double getWallet() { return wallet; }
}

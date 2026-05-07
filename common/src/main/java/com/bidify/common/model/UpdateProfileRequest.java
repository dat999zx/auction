package com.bidify.common.model;

public class UpdateProfileRequest {
    private String nickname;
    private Double wallet;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String nickname, Double wallet) {
        this.nickname = nickname;
        this.wallet = wallet;
    }

    public String getNickname() { return nickname; }
    public Double getWallet() { return wallet; }
}

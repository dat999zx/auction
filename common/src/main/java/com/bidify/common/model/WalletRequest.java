package com.bidify.common.model;

public class WalletRequest {
    private double amount;

    // dùng để tạo một đối tượng WalletRequest
    public WalletRequest() {}

    // dùng để tạo một đối tượng WalletRequest
    public WalletRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() { return amount; }
}

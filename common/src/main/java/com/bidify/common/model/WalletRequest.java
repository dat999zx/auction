package com.bidify.common.model;

public class WalletRequest {
    private double amount;

    // dùng để tạo một đối tượng WalletRequest
    public WalletRequest() {}

    // dùng để tạo một đối tượng WalletRequest
    public WalletRequest(double amount) {
        this.amount = amount;
    }

    // dùng để lấy số tiền
    public double getAmount() { return amount; }
}

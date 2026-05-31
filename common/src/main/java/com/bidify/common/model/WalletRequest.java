package com.bidify.common.model;

// Yêu cầu nạp/rút tiền vào ví — admin sẽ duyệt trước khi thực hiện
public class WalletRequest {
    // Số tiền muốn nạp hoặc rút
    private double amount;

    public WalletRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() { return amount; }
}

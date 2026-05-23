package com.bidify.common.model;

public class WalletRequest {
    private double amount;

    public WalletRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() { return amount; }
}

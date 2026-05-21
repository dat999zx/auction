package com.bidify.common.dto;

public class WalletDto {
    private double balance;
    private double lockedBalance;

    // dùng để tạo một đối tượng WalletDto
    public WalletDto(double balance, double lockedBalance) {
        this.balance = balance;
        this.lockedBalance = lockedBalance;
    }
    
    public double getBalance() { return balance; }
    public double getLockedBalance() { return lockedBalance; }
}

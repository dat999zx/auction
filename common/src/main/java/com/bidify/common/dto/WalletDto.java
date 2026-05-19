package com.bidify.common.dto;

public class WalletDto {
    private double balance;
    private double lockedBalance;

    // dùng để tạo một đối tượng WalletDto
    public WalletDto(double balance, double lockedBalance) {
        this.balance = balance;
        this.lockedBalance = lockedBalance;
    }
    
    // dùng để lấy số dư
    public double getBalance() { return balance; }
    // dùng để lấy locked số dư
    public double getLockedBalance() { return lockedBalance; }
}

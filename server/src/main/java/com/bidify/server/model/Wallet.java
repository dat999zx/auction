package com.bidify.server.model;

import com.bidify.common.exception.ValidationException;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.exception.InsufficientBalanceException;

// Ví tiền của user — quản lý số dư và phần tiền đang bị khóa do đang là bid cao nhất
public class Wallet {
    private volatile double balance; // tổng tiền đang có
    private volatile double lockedBalance; // lockedBalance là phần tiền đang giữ cho bid cao nhất, không được rút hoặc dùng cho bid khác.

    public Wallet(double balance) {
        this.balance = balance;
        this.lockedBalance = 0;
    }
    
    // Khóa một phần số dư khi đặt giá bid.
    public synchronized void lockBalance(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Lock balance amount");
        if (amount > getAvailableBalance())
            throw new InsufficientBalanceException("Insufficient available balance");
        lockedBalance += amount;
    }
    
    // Giải phóng số dư bị khóa khi có người đặt giá cao hơn.
    public synchronized void unlockBalance(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Unlock balance amount");
        if (amount > lockedBalance)
            throw new InsufficientBalanceException("Insufficient locked balance");
        lockedBalance -= amount;
    }
    
    // Thanh toán tiền khi thắng đấu giá — trừ cả balance lẫn lockedBalance cùng lúc
    public synchronized void payWinAuction(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Pay amount");
        if (lockedBalance < amount)
            throw new InsufficientBalanceException("Insufficient locked balance");
        balance -= amount;
        lockedBalance -= amount;
    }
    
    // Nạp tiền vào ví (admin duyệt xong mới gọi hàm này)
    public synchronized void deposit(double amount) {
        if (amount <= 0)
            throw new ValidationException("Deposit amount must be positive");
        balance += amount;
    }
    
    // Rút tiền khỏi ví — chỉ rút được phần available (không đụng vào tiền đang khóa)
    public synchronized void withdraw(double amount) {
        if (amount <= 0)
            throw new ValidationException("Withdraw amount must be positive");
        if (amount > getAvailableBalance())
            throw new InsufficientBalanceException("Insufficient available balance");
        balance -= amount;
    }
    
    public double getBalance() { return balance; }
    public double getAvailableBalance() { return balance - lockedBalance; }
    public void setlockedBalance(double amount) { this.lockedBalance = amount; }
    public double getLockedBalance() { return lockedBalance; }
}

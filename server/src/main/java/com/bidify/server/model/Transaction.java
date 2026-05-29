package com.bidify.server.model;

import com.bidify.common.enums.TransactionType;
import com.bidify.common.utility.IdGenerator;
import com.bidify.common.utility.TimeUtil;
import java.time.LocalDateTime;

// Lịch sử giao dịch tiền của user (nạp, rút, thanh toán đấu giá, nhận tiền bán)
public class Transaction extends Entity {
    // Username chủ giao dịch
    private String username;
    // Loại giao dịch: DEPOSIT, WITHDRAW, PAYMENT, INCOME...
    private TransactionType type;
    // Số tiền giao dịch
    private double amount;
    // ID phiên đấu giá liên quan (null nếu là giao dịch nạp/rút thường)
    private String auctionId;

    // nạp tiền / rút tiền
    public Transaction(String username, TransactionType type, double amount) {
        this(username, type, amount, null);
    }

    // thanh toán / nhận tiền trong auction
    public Transaction(String username, TransactionType type, double amount, String auctionId) {
        super(IdGenerator.genTransactionId(), TimeUtil.nowInVietnam());
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
    }

    // load từ db
    public Transaction(String id, LocalDateTime createdAt, String username, TransactionType type, double amount, String auctionId) {
        super(id, createdAt);
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
    }

    public String getUsername() { return username; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public String getAuctionId() { return auctionId; }
}
